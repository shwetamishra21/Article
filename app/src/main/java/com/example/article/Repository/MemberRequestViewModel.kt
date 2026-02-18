package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class MemberRequestViewModel(
    private val repository: ServiceRequestRepository = ServiceRequestRepository()
) : ViewModel() {

    private val _requests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val requests: StateFlow<List<ServiceRequest>> = _requests.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    private var listenerJob: Job? = null
    private var currentMemberId: String? = null

    companion object {
        private const val TAG = "MemberRequestVM"
    }

    fun loadMemberRequests(memberId: String) {
        if (memberId == currentMemberId && listenerJob?.isActive == true) return

        currentMemberId = memberId
        listenerJob?.cancel()

        listenerJob = viewModelScope.launch {
            _loading.value = true
            try {
                repository.getMemberRequests(memberId).collect { list ->
                    _requests.value = list
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMemberRequests error", e)
                _error.value = e.message ?: "Failed to load requests"
                _loading.value = false
            }
        }
    }

    fun refreshRequests() {
        currentMemberId?.let { memberId ->
            listenerJob?.cancel()
            currentMemberId = null
            loadMemberRequests(memberId)
        }
    }

    fun createRequest(
        title: String,
        description: String,
        serviceType: String,
        preferredDate: Date?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                _error.value = "You must be logged in to create a request"
                _loading.value = false
                return@launch
            }

            val memberId = firebaseUser.uid
            val memberName = UserSessionManager.currentUser.value?.name
                ?: firebaseUser.displayName
                ?: "Member"

            val memberNeighborhood = try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(memberId)
                    .get()
                    .await()
                    .getString("neighborhood") ?: ""
            } catch (e: Exception) {
                Log.w(TAG, "Could not load neighborhood", e)
                ""
            }

            val request = ServiceRequest(
                title = title.trim(),
                description = description.trim(),
                serviceType = serviceType,
                memberId = memberId,
                memberName = memberName,
                memberNeighborhood = memberNeighborhood,
                preferredDate = preferredDate?.let { Timestamp(it) },
                status = ServiceRequest.STATUS_PENDING,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            val result = repository.createRequest(request)

            _loading.value = false
            if (result.isSuccess) {
                Log.d(TAG, "Request created: ${result.getOrNull()}")
                _submitSuccess.value = true
                onSuccess()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to submit request"
                Log.e(TAG, msg)
                _error.value = msg
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            val memberId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            val request = _requests.value.find { it.id == requestId }
            if (request != null &&
                request.status != ServiceRequest.STATUS_PENDING &&
                request.status != ServiceRequest.STATUS_ACCEPTED
            ) {
                _error.value = "Cannot cancel a request that is already ${request.status}"
                return@launch
            }

            val result = repository.cancelRequest(requestId, memberId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to cancel request"
                Log.e(TAG, "cancelRequest error", result.exceptionOrNull())
            }
        }
    }

    fun rateRequest(requestId: String, rating: Float, review: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val firestore = FirebaseFirestore.getInstance()

                val requestDoc = firestore.collection("service_requests")
                    .document(requestId)
                    .get()
                    .await()

                val providerId = requestDoc.getString("providerId")
                if (providerId == null) {
                    _error.value = "No provider assigned to this request"
                    return@launch
                }

                firestore.collection("service_requests")
                    .document(requestId)
                    .update(
                        mapOf(
                            "rating" to rating,
                            "review" to review,
                            "ratedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()

                recalculateProviderRating(providerId)
                refreshRequests()

            } catch (e: Exception) {
                _error.value = "Failed to submit rating: ${e.message}"
                Log.e(TAG, "rateRequest error", e)
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun recalculateProviderRating(providerId: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            val completedRequests = firestore.collection("service_requests")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "completed")
                .get()
                .await()

            val ratings = completedRequests.documents
                .mapNotNull { doc -> doc.getDouble("rating")?.toFloat() }

            if (ratings.isNotEmpty()) {
                val averageRating = ratings.average().toFloat()
                val ratingCount = ratings.size

                firestore.collection("users")
                    .document(providerId)
                    .update(
                        mapOf(
                            "averageRating" to averageRating,
                            "ratingCount" to ratingCount,
                            "lastRatingUpdate" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recalculate provider rating", e)
        }
    }

    fun setError(msg: String) { _error.value = msg }
    fun clearError() { _error.value = null }
    fun clearSubmitSuccess() { _submitSuccess.value = false }

    override fun onCleared() {
        super.onCleared()
        listenerJob?.cancel()
    }
}