package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    companion object {
        private const val TAG = "MemberRequestVM"
    }

    /**
     * Start a real-time stream of this member's requests.
     */
    fun loadMemberRequests(memberId: String) {
        viewModelScope.launch {
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

    /**
     * Create a new service request.
     * Pulls member info from UserSessionManager automatically.
     */
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

            // Resolve member identity from FirebaseAuth (uid) + UserSessionManager (name)
            // and neighbourhood from Firestore — consistent with how every other screen works
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

            // Load neighbourhood from Firestore (non-critical — empty string if missing)
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

    /**
     * Cancel a request owned by this member.
     * Allowed for: pending and accepted (but not in_progress or completed).
     */
    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            val memberId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Guard client-side — repo also enforces this server-side
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

    /**
     * Rate a completed request.
     */
    fun rateRequest(requestId: String, rating: Float, review: String) {
        viewModelScope.launch {
            val memberId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val result = repository.rateRequest(requestId, memberId, rating, review)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to submit rating"
                Log.e(TAG, "rateRequest error", result.exceptionOrNull())
            }
        }
    }

    fun setError(msg: String) {
        _error.value = msg
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSubmitSuccess() {
        _submitSuccess.value = false
    }
}