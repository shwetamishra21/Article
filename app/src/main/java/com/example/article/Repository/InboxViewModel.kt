package com.example.article.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InboxUiState(
    val chats: List<ChatThread> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class InboxViewModel : ViewModel() {

    private companion object {
        const val TAG = "InboxViewModel"
    }

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    private var inboxJob: Job? = null
    private var currentUserId: String? = null

    fun loadInbox(userId: String) {
        if (userId == currentUserId && inboxJob?.isActive == true) return

        currentUserId = userId
        inboxJob?.cancel()

        inboxJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                ChatRepository.observeInbox(userId).collect { chats ->
                    _uiState.value = _uiState.value.copy(
                        chats = chats,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading inbox", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load inbox"
                )
            }
        }
    }

    suspend fun createOrGetChat(
        currentUserId: String,
        otherUserId: String,
        currentUserName: String,
        otherUserName: String,
        currentUserPhoto: String = "",
        otherUserPhoto: String = "",
        currentUserRole: String = "member",
        otherUserRole: String = "member",
        type: String = ChatThread.TYPE_MEMBER,
        serviceRequestId: String? = null
    ): Result<String> {
        return ChatRepository.getOrCreateChat(
            userId1 = currentUserId,
            userId2 = otherUserId,
            user1Name = currentUserName,
            user2Name = otherUserName,
            user1Photo = currentUserPhoto,
            user2Photo = otherUserPhoto,
            user1Role = currentUserRole,
            user2Role = otherUserRole,
            type = type,
            serviceRequestId = serviceRequestId
        )
    }

    override fun onCleared() {
        super.onCleared()
        inboxJob?.cancel()
    }
}