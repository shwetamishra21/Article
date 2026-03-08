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

    // Pending requests from ALL provider neighbourhoods combined
    private val _pendingRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val pendingRequests: StateFlow<List<ServiceRequest>> = _pendingRequests.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _pendingLoading = MutableStateFlow(false)
    val pendingLoading: StateFlow<Boolean> = _pendingLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // All neighbourhoods this provider has been approved into (replaces single _providerNeighbourhood)
    private val _providerNeighbourhoods = MutableStateFlow<List<Neighbourhood>>(emptyList())
    val providerNeighbourhoods: StateFlow<List<Neighbourhood>> = _providerNeighbourhoods.asStateFlow()

    // True when the provider has no approved neighbourhood at all
    private val _noNeighbourhood = MutableStateFlow(false)
    val noNeighbourhood: StateFlow<Boolean> = _noNeighbourhood.asStateFlow()

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
     * Load open/pending requests from ALL neighbourhoods this provider belongs to.
     *
     * Flow:
     *  1. Query join_requests for this provider where status == "approved"
     *  2. If none → set _noNeighbourhood = true, show join prompt
     *  3. For each approved neighbourhood, fetch its member UIDs
     *  4. Merge all UIDs into one deduplicated set
     *  5. Subscribe to pending requests whose memberId is in that merged set
     *
     * Pass serviceType to further narrow results to a matching trade.
     */
    fun loadPendingRequests(serviceType: String? = null) {
        viewModelScope.launch {
            _pendingLoading.value = true
            _noNeighbourhood.value = false

            val providerId = UserSessionManager.getCurrentUid()
            if (providerId == null) {
                _noNeighbourhood.value = true
                _pendingLoading.value = false
                return@launch
            }

            // Step 1: get all approved neighbourhoods for this provider
            val neighResult = NeighbourhoodRepository.getApprovedNeighbourhoodsForProvider(providerId)
            val neighbourhoods = neighResult.getOrNull() ?: emptyList()

            if (neighbourhoods.isEmpty()) {
                Log.d(TAG, "Provider has no approved neighbourhoods — showing join prompt")
                _noNeighbourhood.value = true
                _providerNeighbourhoods.value = emptyList()
                _pendingRequests.value = emptyList()
                _pendingLoading.value = false
                return@launch
            }

            _providerNeighbourhoods.value = neighbourhoods
            Log.d(TAG, "Provider is in ${neighbourhoods.size} neighbourhood(s): ${neighbourhoods.map { it.name }}")

            // Step 2: collect member UIDs from all neighbourhoods (deduplicated)
            val allMemberUids = mutableSetOf<String>()
            for (neighbourhood in neighbourhoods) {
                val uidsResult = NeighbourhoodRepository.getMemberUidsOfNeighbourhood(neighbourhood.id)
                uidsResult.getOrNull()?.let { allMemberUids.addAll(it) }
            }

            Log.d(TAG, "Total unique member UIDs across all neighbourhoods: ${allMemberUids.size}")

            if (allMemberUids.isEmpty()) {
                _pendingRequests.value = emptyList()
                _pendingLoading.value = false
                return@launch
            }

            // Step 3: subscribe to live pending requests filtered to those UIDs
            try {
                repository.getPendingRequestsForNeighbourhood(allMemberUids.toList(), serviceType)
                    .collect { list ->
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

    fun startWork(requestId: String) {
        viewModelScope.launch {
            val result = repository.startWork(requestId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to start work"
                Log.e(TAG, "startWork error", result.exceptionOrNull())
            }
        }
    }

    fun complete(requestId: String, providerId: String) {
        viewModelScope.launch {
            val result = repository.completeRequest(requestId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to complete request"
                Log.e(TAG, "complete error", result.exceptionOrNull())
            }
        }
    }
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