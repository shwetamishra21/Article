package com.example.article.Repository

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.FeedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val bio: String = "",
    val neighborhood: String = "",  // ADDED
    val photoUrl: String = "",
    val email: String = "",
    val role: String = "member"
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: UserProfile,
        val posts: List<FeedItem.Post>
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    /* ==================== LOAD PROFILE ==================== */

    fun loadProfile() {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = ProfileUiState.Error("Not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Loading

                // Fetch user profile
                val profileDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val profile = UserProfile(
                    uid = userId,
                    name = profileDoc.getString("name") ?: "",
                    bio = profileDoc.getString("bio") ?: "",
                    neighborhood = profileDoc.getString("neighborhood") ?: "",  // ADDED
                    photoUrl = profileDoc.getString("photoUrl") ?: "",
                    email = profileDoc.getString("email") ?: auth.currentUser?.email ?: "",
                    role = profileDoc.getString("role") ?: "member"
                )

                // ✅ NO INDEX REQUIRED - Query only by authorId, sort in memory
                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .get()
                    .await()

                val posts = postsSnapshot.documents
                    .mapNotNull { doc ->
                        try {
                            // Filter out announcements - only show posts
                            val type = doc.getString("type") ?: return@mapNotNull null
                            if (type != "post") return@mapNotNull null

                            val timestamp = when (val createdAt = doc.get("createdAt")) {
                                is com.google.firebase.Timestamp -> createdAt.toDate().time
                                is Long -> createdAt
                                else -> System.currentTimeMillis()
                            }

                            FeedItem.Post(
                                id = doc.id,
                                author = doc.getString("authorName") ?: "",
                                content = doc.getString("content") ?: "",
                                time = timestamp,
                                likes = (doc.getLong("likes") ?: 0L).toInt(),
                                commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                                likedByMe = false,
                                imageUrl = doc.getString("imageUrl")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .sortedByDescending { it.time }  // Sort client-side by timestamp
                    .take(20)  // Limit to 20 most recent posts

                _uiState.value = ProfileUiState.Success(profile, posts)

            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.localizedMessage ?: "Failed to load profile"
                )
            }
        }
    }

    /* ==================== GET NEIGHBORHOOD (Helper) ==================== */

    suspend fun getUserNeighborhood(): String {
        return try {
            val userId = auth.currentUser?.uid ?: return "Your Neighborhood"

            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            userDoc.getString("neighborhood")?.takeIf { it.isNotBlank() } ?: "Your Neighborhood"
        } catch (e: Exception) {
            "Your Neighborhood"
        }
    }

    /* ==================== UPDATE PROFILE ==================== */

    fun updateProfile(name: String, bio: String, neighborhood: String, onComplete: () -> Unit) {  // ADDED neighborhood parameter
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isUpdating.value = true

                firestore.collection("users")
                    .document(userId)
                    .update(
                        mapOf(
                            "name" to name,
                            "bio" to bio,
                            "neighborhood" to neighborhood,  // ADDED
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()

                // Update current state
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(
                        profile = currentState.profile.copy(
                            name = name,
                            bio = bio,
                            neighborhood = neighborhood  // ADDED
                        )
                    )
                }

                _isUpdating.value = false
                onComplete()

            } catch (e: Exception) {
                _isUpdating.value = false
                _uiState.value = ProfileUiState.Error(
                    e.localizedMessage ?: "Failed to update profile"
                )
            }
        }
    }

    /* ==================== UPLOAD PROFILE IMAGE ==================== */

    fun uploadProfileImage(uri: Uri, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isUpdating.value = true

                // Upload to Storage
                val storageRef = storage.reference
                    .child("profile_images/$userId/profile.jpg")

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Update Firestore
                firestore.collection("users")
                    .document(userId)
                    .update("photoUrl", downloadUrl)
                    .await()

                // Update current state
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(
                        profile = currentState.profile.copy(photoUrl = downloadUrl)
                    )
                }

                _isUpdating.value = false
                onComplete()

            } catch (e: Exception) {
                _isUpdating.value = false
                _uiState.value = ProfileUiState.Error(
                    e.localizedMessage ?: "Failed to upload image"
                )
            }
        }
    }

    /* ==================== DELETE POST ==================== */

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts")
                    .document(postId)
                    .delete()
                    .await()

                // Update UI state
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(
                        posts = currentState.posts.filterNot { it.id == postId }
                    )
                }

            } catch (e: Exception) {
                // Silently fail or show toast
            }
        }
    }
}