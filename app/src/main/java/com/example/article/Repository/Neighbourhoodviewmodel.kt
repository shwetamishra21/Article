package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==================== JOIN REQUESTS VIEW MODEL (Admin) ====================

class JoinRequestViewModel : ViewModel() {

    private val _requests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val requests: StateFlow<List<JoinRequest>> = _requests.asStateFlow()

    private val _neighbourhood = MutableStateFlow<Neighbourhood?>(null)
    val neighbourhood: StateFlow<Neighbourhood?> = _neighbourhood.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    companion object {
        private const val TAG = "JoinRequestViewModel"
    }

    /**
     * Load the admin's neighbourhood, then load pending requests for it.
     */
    fun loadForAdmin() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val adminId = UserSessionManager.getCurrentUid()
            if (adminId == null) {
                _error.value = "Not logged in"
                _isLoading.value = false
                return@launch
            }

            // Step 1: get admin's neighbourhood
            val neighResult = NeighbourhoodRepository.getNeighbourhoodByAdmin(adminId)
            if (neighResult.isFailure || neighResult.getOrNull() == null) {
                _error.value = "No neighbourhood found. Create one first."
                _isLoading.value = false
                return@launch
            }

            val neighbourhood = neighResult.getOrNull()!!
            _neighbourhood.value = neighbourhood

            // Step 2: load pending requests
            loadRequests(neighbourhood.id)
        }
    }

    private suspend fun loadRequests(neighbourhoodId: String) {
        val result = NeighbourhoodRepository.getPendingRequests(neighbourhoodId)
        if (result.isSuccess) {
            _requests.value = result.getOrNull() ?: emptyList()
            Log.d(TAG, "Loaded ${_requests.value.size} pending requests")
        } else {
            _error.value = result.exceptionOrNull()?.message ?: "Failed to load requests"
        }
        _isLoading.value = false
    }

    fun approveRequest(request: JoinRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = NeighbourhoodRepository.approveRequest(request)
            if (result.isSuccess) {
                _successMessage.value = "${request.userName} approved successfully"
                // Refresh list
                _neighbourhood.value?.id?.let { loadRequests(it) }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to approve"
            }
            _isLoading.value = false
        }
    }

    fun rejectRequest(request: JoinRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = NeighbourhoodRepository.rejectRequest(request.id)
            if (result.isSuccess) {
                _successMessage.value = "${request.userName} rejected"
                _neighbourhood.value?.id?.let { loadRequests(it) }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to reject"
            }
            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}

// ==================== NEIGHBOURHOOD SEARCH VIEW MODEL (Members/Providers) ====================

sealed class NeighbourhoodSearchState {
    object Idle : NeighbourhoodSearchState()
    object Loading : NeighbourhoodSearchState()
    data class Success(val neighbourhoods: List<Neighbourhood>) : NeighbourhoodSearchState()
    data class Error(val message: String) : NeighbourhoodSearchState()
}

class NeighbourhoodSearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NeighbourhoodSearchState>(NeighbourhoodSearchState.Idle)
    val uiState: StateFlow<NeighbourhoodSearchState> = _uiState.asStateFlow()

    // Map of neighbourhoodId -> request status ("none" | "pending" | "approved" | "rejected")
    private val _requestStatusMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val requestStatusMap: StateFlow<Map<String, String>> = _requestStatusMap.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isActioning = MutableStateFlow(false)
    val isActioning: StateFlow<Boolean> = _isActioning.asStateFlow()

    private var allNeighbourhoods: List<Neighbourhood> = emptyList()
    private var currentQuery: String = ""

    companion object {
        private const val TAG = "NeighbourhoodSearchVM"
    }

    fun loadNeighbourhoods() {
        viewModelScope.launch {
            _uiState.value = NeighbourhoodSearchState.Loading

            val result = NeighbourhoodRepository.getAllNeighbourhoods()
            if (result.isSuccess) {
                allNeighbourhoods = result.getOrNull() ?: emptyList()
                applySearch()

                // Load request statuses for current user
                loadRequestStatuses()
            } else {
                _uiState.value = NeighbourhoodSearchState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load neighbourhoods"
                )
            }
        }
    }

    private suspend fun loadRequestStatuses() {
        val userId = UserSessionManager.getCurrentUid() ?: return
        val statusMap = mutableMapOf<String, String>()

        for (neighbourhood in allNeighbourhoods) {
            val status = NeighbourhoodRepository.getUserRequestStatus(userId, neighbourhood.id)
            statusMap[neighbourhood.id] = status
        }

        _requestStatusMap.value = statusMap
    }

    fun searchNeighbourhoods(query: String) {
        currentQuery = query
        applySearch()
    }

    private fun applySearch() {
        val filtered = if (currentQuery.isBlank()) {
            allNeighbourhoods
        } else {
            allNeighbourhoods.filter {
                it.name.contains(currentQuery, ignoreCase = true) ||
                        it.description.contains(currentQuery, ignoreCase = true)
            }
        }
        _uiState.value = NeighbourhoodSearchState.Success(filtered)
    }

    fun sendJoinRequest(neighbourhood: Neighbourhood) {
        viewModelScope.launch {
            _isActioning.value = true
            val user = UserSessionManager.currentUser.value
            if (user == null) {
                _actionMessage.value = "Not logged in"
                _isActioning.value = false
                return@launch
            }

            val result = NeighbourhoodRepository.sendJoinRequest(
                userId = user.uid,
                userName = user.name,
                userEmail = user.email,
                userRole = user.role,
                neighbourhoodId = neighbourhood.id
            )

            if (result.isSuccess) {
                _actionMessage.value = "Request sent to ${neighbourhood.name}"
                // Update local status map immediately so button reflects change
                _requestStatusMap.value = _requestStatusMap.value.toMutableMap().apply {
                    put(neighbourhood.id, "pending")
                }
            } else {
                _actionMessage.value = result.exceptionOrNull()?.message ?: "Failed to send request"
            }
            _isActioning.value = false
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}

// ==================== ADMIN NEIGHBOURHOOD SETUP VIEW MODEL ====================

class AdminNeighbourhoodViewModel : ViewModel() {

    private val _neighbourhood = MutableStateFlow<Neighbourhood?>(null)
    val neighbourhood: StateFlow<Neighbourhood?> = _neighbourhood.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun loadAdminNeighbourhood() {
        viewModelScope.launch {
            _isLoading.value = true
            val adminId = UserSessionManager.getCurrentUid() ?: return@launch
            val result = NeighbourhoodRepository.getNeighbourhoodByAdmin(adminId)
            _neighbourhood.value = result.getOrNull()
            _isLoading.value = false
        }
    }

    fun createNeighbourhood(name: String, description: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _message.value = "Name cannot be empty"
                return@launch
            }
            _isLoading.value = true
            val adminId = UserSessionManager.getCurrentUid()
            if (adminId == null) {
                _message.value = "Not logged in"
                _isLoading.value = false
                return@launch
            }

            val result = NeighbourhoodRepository.createNeighbourhood(adminId, name.trim(), description.trim())
            if (result.isSuccess) {
                _message.value = "Neighbourhood created successfully!"
                loadAdminNeighbourhood() // Refresh
            } else {
                _message.value = result.exceptionOrNull()?.message ?: "Failed to create neighbourhood"
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}