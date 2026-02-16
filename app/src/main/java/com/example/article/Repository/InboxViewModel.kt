package com.example.article.chat

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

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    private var inboxListener: Job? = null

    // ==================== LOAD INBOX ====================

    fun loadInbox(userId: String) {
        if (inboxListener != null) return

        _uiState.value = InboxUiState(isLoading = true)

        inboxListener = viewModelScope.launch {
            ChatRepository.observeInbox(userId).collect { chats ->
                _uiState.value = InboxUiState(
                    chats = chats,
                    isLoading = false
                )
            }
        }
    }

    // ==================== CREATE CHAT ====================

    suspend fun createOrGetChat(
        currentUserId: String,
        otherUserId: String,
        currentUserName: String,
        otherUserName: String,
        currentUserPhoto: String = "",
        otherUserPhoto: String = "",
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
            type = type,
            serviceRequestId = serviceRequestId
        )
    }

    // ==================== CLEANUP ====================

    override fun onCleared() {
        super.onCleared()
        inboxListener?.cancel()
    }
}