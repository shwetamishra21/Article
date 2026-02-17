package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProviderRequestsViewModel(
    private val repository: ServiceRequestRepository = ServiceRequestRepository()
) : ViewModel() {

    // Provider's assigned requests (accepted / in_progress / completed)
    private val _requests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val requests: StateFlow<List<ServiceRequest>> = _requests.asStateFlow()

    // Pending requests available to claim
    private val _pendingRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val pendingRequests: StateFlow<List<ServiceRequest>> = _pendingRequests.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _pendingLoading = MutableStateFlow(false)
    val pendingLoading: StateFlow<Boolean> = _pendingLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        private const val TAG = "ProviderRequestsVM"
    }

    /**
     * Load all requests assigned to this provider (real-time).
     */
    fun loadRequests(providerId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.getProviderRequests(providerId).collect { list ->
                    _requests.value = list
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadRequests error", e)
                _error.value = e.message ?: "Failed to load requests"
                _loading.value = false
            }
        }
    }

    /**
     * Load open/pending requests the provider can claim.
     * Pass provider's service type to show only matching requests.
     */
    fun loadPendingRequests(serviceType: String? = null) {
        viewModelScope.launch {
            _pendingLoading.value = true
            try {
                repository.getPendingRequests(serviceType).collect { list ->
                    _pendingRequests.value = list
                    _pendingLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPendingRequests error", e)
                _error.value = e.message ?: "Failed to load available requests"
                _pendingLoading.value = false
            }
        }
    }

    /**
     * Accept a pending request. Pulls provider info from UserSessionManager.
     */
    fun accept(requestId: String, providerId: String) {
        viewModelScope.launch {
            val providerName = UserSessionManager.currentUser.value?.name
                ?: FirebaseAuth.getInstance().currentUser?.displayName
                ?: "Provider"
            val result = repository.acceptRequest(requestId, providerId, providerName)
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message ?: "Failed to accept request"
                _error.value = msg
                Log.e(TAG, "accept error: $msg")
            } else {
                Log.d(TAG, "Request $requestId accepted")
            }
        }
    }

    /**
     * Start working — transitions accepted → in_progress.
     */
    fun startWork(requestId: String) {
        viewModelScope.launch {
            val result = repository.startWork(requestId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to start work"
                Log.e(TAG, "startWork error", result.exceptionOrNull())
            }
        }
    }

    /**
     * Mark request as completed.
     */
    fun complete(requestId: String, providerId: String) {
        viewModelScope.launch {
            val result = repository.completeRequest(requestId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to complete request"
                Log.e(TAG, "complete error", result.exceptionOrNull())
            }
        }
    }
    /**
     * Decline an accepted/in_progress request — resets it to pending.
     */
    fun decline(requestId: String) {
        viewModelScope.launch {
            val result = repository.declineRequest(requestId, byProvider = true)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to decline request"
                Log.e(TAG, "decline error", result.exceptionOrNull())
            }
        }
    }
    fun clearError() {
        _error.value = null
    }
}