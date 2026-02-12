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
import android. content. Context
import kotlinx.coroutines.launch
import com. example. article. utils. CloudinaryHelper
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
                                authorId = doc.getString("authorId") ?: userId,
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
    fun updateProfile(
        name: String,
        bio: String,
        neighborhood: String,
        onComplete: () -> Unit
    ) {
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
                            "neighborhood" to neighborhood
                        )
                    )
                    .await()

                loadProfile() // refresh UI
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


    /* ==================== UPLOAD PROFILE IMAGE ==================== */

    fun uploadProfileImage(uri: Uri, context: Context, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isUpdating.value = true

                if (!CloudinaryHelper.isConfigured()) {
                    _isUpdating.value = false
                    _uiState.value = ProfileUiState.Error("Cloudinary not configured")
                    return@launch
                }

                val uploadResult = CloudinaryHelper.uploadImage(
                    imageUri = uri,
                    context = context,
                    folder = "profiles/$userId"
                )

                if (uploadResult.isFailure) {
                    _isUpdating.value = false
                    _uiState.value =
                        ProfileUiState.Error(uploadResult.exceptionOrNull()?.message ?: "Upload failed")
                    return@launch
                }

                val result = uploadResult.getOrNull()!!

                firestore.collection("users")
                    .document(userId)
                    .update(
                        mapOf(
                            "photoUrl" to result.secureUrl,
                            "photoPublicId" to result.publicId
                        )
                    )
                    .await()

                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(
                        profile = currentState.profile.copy(
                            photoUrl = result.secureUrl
                        )
                    )
                }

                _isUpdating.value = false
                onComplete()

            } catch (e: Exception) {
                _isUpdating.value = false
                _uiState.value =
                    ProfileUiState.Error(e.localizedMessage ?: "Failed to upload image")
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