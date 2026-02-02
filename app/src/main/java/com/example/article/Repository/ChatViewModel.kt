package com.example.article

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ChatViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    fun observeMessages(chatId: String) {
        listener?.remove()

        _uiState.update { it.copy(isLoading = true) }

        listener = firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                    return@addSnapshotListener
                }

                val messages: List<ChatMessage> =
                    snapshot?.documents
                        ?.mapNotNull { doc ->
                            doc.toObject(ChatMessage::class.java)
                                ?.copy(id = doc.id)
                        }
                        ?: emptyList()

                _uiState.update {
                    it.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            }
    }

    fun sendMessage(
        chatId: String,
        text: String,
        senderId: String
    ) {
        if (text.isBlank()) return

        val data = hashMapOf(
            "text" to text,
            "senderId" to senderId,
            "timestamp" to System.currentTimeMillis()
        )

        firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(data)
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
