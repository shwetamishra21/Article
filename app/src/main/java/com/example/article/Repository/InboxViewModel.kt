package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.ChatThread
import com.example.article.core.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InboxViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ChatThread>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ChatThread>>> = _uiState

    private var currentUserId: String? = null

    companion object {
        private const val TAG = "InboxViewModel"
    }

    /**
     * Start observing inbox for logged-in user
     */
    fun loadInbox(userId: String) {
        if (currentUserId == userId) {
            Log.d(TAG, "Already observing inbox for user: $userId")
            return
        }

        currentUserId = userId
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                ChatRepository.observeInbox(userId).collect { chats ->
                    _uiState.value = UiState.Success(chats)
                    Log.d(TAG, "Inbox updated with ${chats.size} chats")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading inbox", e)
                _uiState.value = UiState.Error(
                    e.message ?: "Failed to load inbox"
                )
            }
        }
    }

    /**
     * Refresh inbox
     */
    fun refresh() {
        currentUserId?.let { userId ->
            Log.d(TAG, "Refreshing inbox for user: $userId")
            currentUserId = null // Force reload
            loadInbox(userId)
        }
    }

    /**
     * Alias for start method (for compatibility)
     */
    fun start(userId: String) {
        loadInbox(userId)
    }

    override fun onCleared() {
        Log.d(TAG, "InboxViewModel cleared")
        super.onCleared()
    }
}