package com.example.article.Repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.Repository.RequestRepository
import com.example.article.Repository.ServiceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProviderRequestsViewModel : ViewModel() {

    private val _requests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val requests: StateFlow<List<ServiceRequest>> = _requests

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadRequests(providerId: String) {
        if (_loading.value) return

        viewModelScope.launch {
            _loading.value = true
            _requests.value = RequestRepository.getProviderRequests(providerId)
            _loading.value = false
        }
    }

    fun accept(requestId: String, providerId: String) {
        viewModelScope.launch {
            RequestRepository.acceptRequest(requestId, providerId)
            loadRequests(providerId)
        }
    }

    fun complete(requestId: String, providerId: String) {
        viewModelScope.launch {
            RequestRepository.completeRequest(requestId)
            loadRequests(providerId)
        }
    }
}
