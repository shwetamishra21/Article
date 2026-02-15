package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.UserSessionManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    companion object {
        private const val TAG = "MemberRequestVM"
    }

    /**
     * Load all requests created by this member
     */
    fun loadMemberRequests(memberId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                repository.getMemberRequests(memberId).collect { requestList ->
                    _requests.value = requestList
                    _loading.value = false
                    Log.d(TAG, "Loaded ${requestList.size} member requests")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading member requests", e)
                _error.value = e.message ?: "Failed to load requests"
                _loading.value = false
            }
        }
    }

    /**
     * Create a new service request
     */
    fun createRequest(
        title: String,
        description: String,
        serviceType: String,
        preferredDate: Date?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val currentUser = UserSessionManager.currentUser.value
                if (currentUser == null) {
                    _error.value = "Not logged in"
                    _loading.value = false
                    return@launch
                }

                val request = ServiceRequest(
                    title = title,
                    description = description,
                    serviceType = serviceType,
                    memberId = currentUser.uid,  // ✅ CORRECT                    memberName = currentUser.name,
                    memberNeighborhood = currentUser.neighbourhood ?: "Unknown",
                    preferredDate = preferredDate?.let { Timestamp(it) },
                    status = ServiceRequest.STATUS_PENDING
                )

                val result = repository.createRequest(request)

                if (result.isSuccess) {
                    Log.d(TAG, "Request created successfully")
                    _loading.value = false
                    onSuccess()
                } else {
                    _error.value = "Failed to create request"
                    _loading.value = false
                    Log.e(TAG, "Create request failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating request", e)
                _error.value = e.message ?: "Failed to create request"
                _loading.value = false
            }
        }
    }

    /**
     * Cancel a pending request
     */
    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = repository.declineRequest(requestId, byProvider = false)

                if (result.isFailure) {
                    _error.value = "Failed to cancel request"
                    Log.e(TAG, "Cancel failed", result.exceptionOrNull())
                } else {
                    Log.d(TAG, "Request $requestId cancelled successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling request", e)
                _error.value = e.message ?: "Failed to cancel request"
            }
        }
    }

    /**
     * Set error message
     */
    fun setError(message: String) {
        _error.value = message
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}