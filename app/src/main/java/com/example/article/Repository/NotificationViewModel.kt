package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.UserSessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationViewModel : ViewModel() {

    private companion object { const val TAG = "NotificationVM" }

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var listenerJob: Job? = null
    private var currentUserId: String? = null

    fun startObserving(userId: String) {
        if (userId == currentUserId && listenerJob?.isActive == true) return
        currentUserId = userId
        listenerJob?.cancel()
        listenerJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                NotificationRepository.observeNotifications(userId).collect { list ->
                    val unread = list.count { !it.read }
                    _uiState.value = _uiState.value.copy(
                        notifications = list,
                        unreadCount   = unread,
                        isLoading     = false
                    )
                    _unreadCount.value = unread
                }
            } catch (e: Exception) {
                Log.e(TAG, "observe error", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun startObservingFromSession() {
        val uid = UserSessionManager.currentUser.value?.uid ?: return
        startObserving(uid)
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            NotificationRepository.markAsRead(notificationId)
                .onFailure { Log.e(TAG, "markAsRead failed", it) }
        }
    }

    fun markAllAsRead() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            NotificationRepository.markAllAsRead(uid)
                .onFailure { Log.e(TAG, "markAllAsRead failed", it) }
        }
    }

    fun delete(notificationId: String) {
        viewModelScope.launch {
            NotificationRepository.deleteNotification(notificationId)
                .onFailure { Log.e(TAG, "delete failed", it) }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    override fun onCleared() {
        super.onCleared()
        listenerJob?.cancel()
    }
}