package com.example.article

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.Repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSending: Boolean = false,
    val isDeleting: Boolean = false  // ✅ NEW: Delete loading state
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var currentChatId: String? = null

    companion object {
        private const val TAG = "ChatViewModel"
    }

    /**
     * Start observing messages for a chat
     */
    fun observeMessages(chatId: String) {
        if (currentChatId == chatId) {
            Log.d(TAG, "Already observing chat: $chatId")
            return
        }

        currentChatId = chatId
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                ChatRepository.observeMessages(chatId).collect { messages ->
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isLoading = false,
                            error = null
                        )
                    }
                    Log.d(TAG, "Received ${messages.size} messages for chat $chatId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing messages", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
            }
        }
    }

    /**
     * Send a message to the current chat
     */
    fun sendMessage(
        chatId: String,
        text: String,
        senderId: String
    ) {
        if (text.isBlank()) {
            Log.w(TAG, "Attempted to send blank message")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            try {
                val result = ChatRepository.sendMessage(
                    chatId = chatId,
                    text = text.trim(),
                    senderId = senderId
                )

                result.fold(
                    onSuccess = { messageId ->
                        Log.d(TAG, "Message sent successfully: $messageId")
                        _uiState.update { it.copy(isSending = false) }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to send message", error)
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                error = "Failed to send message: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending message", e)
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = "Failed to send message: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * ✅ NEW: Delete a message
     */
    fun deleteMessage(
        chatId: String,
        messageId: String,
        userId: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            try {
                val result = ChatRepository.deleteMessage(
                    chatId = chatId,
                    messageId = messageId,
                    userId = userId
                )

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "✅ Message deleted successfully")
                        _uiState.update { it.copy(isDeleting = false) }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Failed to delete message", error)
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                error = error.message ?: "Failed to delete message"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception while deleting message", e)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete message: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ChatViewModel cleared")
    }
}