package com.example.article.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MemberItem(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val neighbourhood: String = "",
    val isBanned: Boolean = false
)

class MemberManagementViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _members = MutableStateFlow<List<MemberItem>>(emptyList())
    val members: StateFlow<List<MemberItem>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMembers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("role", "member")
                    .get()
                    .await()

                _members.value = snapshot.documents.mapNotNull { doc ->
                    MemberItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        email = doc.getString("email") ?: "",
                        neighbourhood = doc.getString("neighbourhood") ?: "",
                        isBanned = doc.getBoolean("isBanned") ?: false
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load members"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleBan(member: MemberItem) {
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(member.id)
                    .update("isBanned", !member.isBanned)
                    .await()
                loadMembers()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update member"
            }
        }
    }
}