package com.example.article.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val typingUsers: List<String> = emptyList(),
    val otherUserName: String = "",
    val otherUserPhoto: String = ""
)

class ChatViewModel : ViewModel() {

    private companion object {
        const val TAG = "ChatViewModel"
        const val TYPING_TIMEOUT_MS = 3000L
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageListener: Job? = null
    private var typingJob: Job? = null
    private var currentChatId: String? = null
    private var currentUserId: String? = null

    // ==================== OBSERVE MESSAGES ====================

    fun observeMessages(
        chatId: String,
        currentUserId: String,
        otherUserName: String,
        otherUserPhoto: String
    ) {
        this.currentChatId = chatId
        this.currentUserId = currentUserId

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            otherUserName = otherUserName,
            otherUserPhoto = otherUserPhoto
        )

        messageListener?.cancel()
        messageListener = viewModelScope.launch {
            ChatRepository.observeMessages(chatId).collect { messages ->
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false
                )

                // Mark unread messages as read
                markUnreadMessagesAsRead(chatId, currentUserId, messages)
            }
        }

        // Observe chat metadata for typing indicators
        observeChatMetadata(chatId, currentUserId)
    }

    private fun observeChatMetadata(chatId: String, currentUserId: String) {
        viewModelScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener

                        val typingUsers = (snapshot.get("typingUsers") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.filter { it != currentUserId } // Exclude current user
                            ?: emptyList()

                        _uiState.value = _uiState.value.copy(typingUsers = typingUsers)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing chat metadata", e)
            }
        }
    }

    // ==================== SEND MESSAGE ====================

    fun sendMessage(
        chatId: String,
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String,
        text: String,
        recipientId: String
    ) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Stop typing indicator
            setTyping(chatId, currentUserId, false)

            val result = ChatRepository.sendMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderName = currentUserName,
                senderPhotoUrl = currentUserPhoto,
                text = text,
                recipientId = recipientId
            )

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send message"
                )
            }
        }
    }

    // ==================== TYPING INDICATOR ====================

    fun onTyping(chatId: String, userId: String) {
        typingJob?.cancel()

        viewModelScope.launch {
            // Set typing to true
            setTyping(chatId, userId, true)

            // Auto-cancel after timeout
            typingJob = launch {
                delay(TYPING_TIMEOUT_MS)
                setTyping(chatId, userId, false)
            }
        }
    }

    fun onStopTyping(chatId: String, userId: String) {
        typingJob?.cancel()
        viewModelScope.launch {
            setTyping(chatId, userId, false)
        }
    }

    private suspend fun setTyping(chatId: String, userId: String, isTyping: Boolean) {
        ChatRepository.setTyping(chatId, userId, isTyping)
    }

    // ==================== MARK AS READ ====================

    private fun markUnreadMessagesAsRead(
        chatId: String,
        userId: String,
        messages: List<ChatMessage>
    ) {
        viewModelScope.launch {
            val unreadMessageIds = messages
                .filter { !it.isReadBy(userId) && it.senderId != userId }
                .map { it.id }

            if (unreadMessageIds.isNotEmpty()) {
                ChatRepository.markMessagesAsRead(chatId, userId, unreadMessageIds)
            }
        }
    }

    // ==================== CLEAR ERROR ====================

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ==================== CLEANUP ====================

    override fun onCleared() {
        super.onCleared()
        messageListener?.cancel()
        typingJob?.cancel()

        // Clean up typing indicator on exit
        currentChatId?.let { chatId ->
            currentUserId?.let { userId ->
                viewModelScope.launch {
                    setTyping(chatId, userId, false)
                }
            }
        }
    }
}