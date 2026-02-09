package com.example.article.Repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.core.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.article.ChatThread


class InboxViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow<UiState<List<ChatThread>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ChatThread>>> = _uiState

    private var observeJob: Job? = null

    /**
     * Start observing inbox for logged-in user
     */
    fun loadInbox(userId: String) {
        if (observeJob != null) return

        observeJob = viewModelScope.launch {
            ChatRepository.observeInbox(userId).collect { chats ->
                _uiState.value = UiState.Success(chats)
            }
        }
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }
    fun start(userId: String) {
        loadInbox(userId)
    }

}
