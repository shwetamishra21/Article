package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProviderRequestsViewModel(
    private val repository: ServiceRequestRepository = ServiceRequestRepository()
) : ViewModel() {

    private val _requests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val requests: StateFlow<List<ServiceRequest>> = _requests.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        private const val TAG = "ProviderRequestsVM"
    }

    /**
     * Load all requests assigned to this provider
     */
    fun loadRequests(providerId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                repository.getProviderRequests(providerId).collect { requestList ->
                    _requests.value = requestList
                    _loading.value = false
                    Log.d(TAG, "Loaded ${requestList.size} requests")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading requests", e)
                _error.value = e.message ?: "Failed to load requests"
                _loading.value = false
            }
        }
    }

    /**
     * Accept a pending request
     */
    fun accept(requestId: String, providerId: String) {
        viewModelScope.launch {
            try {
                val currentUser = UserSessionManager.currentUser.value
                val providerName = currentUser?.name ?: "Provider"

                val result = repository.acceptRequest(requestId, providerId, providerName)

                if (result.isFailure) {
                    _error.value = "Failed to accept request"
                    Log.e(TAG, "Accept failed", result.exceptionOrNull())
                } else {
                    Log.d(TAG, "Request $requestId accepted successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting request", e)
                _error.value = e.message ?: "Failed to accept request"
            }
        }
    }

    /**
     * Start working on an accepted request
     */
    fun startWork(requestId: String) {
        viewModelScope.launch {
            try {
                val result = repository.startWork(requestId)

                if (result.isFailure) {
                    _error.value = "Failed to start work"
                    Log.e(TAG, "Start work failed", result.exceptionOrNull())
                } else {
                    Log.d(TAG, "Work started on request $requestId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting work", e)
                _error.value = e.message ?: "Failed to start work"
            }
        }
    }

    /**
     * Complete a request
     */
    fun complete(requestId: String, providerId: String) {
        viewModelScope.launch {
            try {
                val result = repository.completeRequest(requestId)

                if (result.isFailure) {
                    _error.value = "Failed to complete request"
                    Log.e(TAG, "Complete failed", result.exceptionOrNull())
                } else {
                    Log.d(TAG, "Request $requestId completed successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error completing request", e)
                _error.value = e.message ?: "Failed to complete request"
            }
        }
    }

    /**
     * Decline a request
     */
    fun decline(requestId: String) {
        viewModelScope.launch {
            try {
                val result = repository.declineRequest(requestId, byProvider = true)

                if (result.isFailure) {
                    _error.value = "Failed to decline request"
                    Log.e(TAG, "Decline failed", result.exceptionOrNull())
                } else {
                    Log.d(TAG, "Request $requestId declined successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error declining request", e)
                _error.value = e.message ?: "Failed to decline request"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}