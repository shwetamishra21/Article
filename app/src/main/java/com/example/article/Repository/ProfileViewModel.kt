package com.example.article.Repository

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.FeedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.content.Context
import kotlinx.coroutines.launch
import com.example.article.utils.CloudinaryHelper
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val bio: String = "",
    val neighbourhood: String = "",
    val photoUrl: String = "",
    val email: String = "",
    val role: String = "member",
    val skills: List<String> = emptyList()
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: UserProfile,
        val posts: List<FeedItem.Post> = emptyList()
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

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    /* ==================== LOAD PROFILE (Own Profile) ==================== */

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
                    neighbourhood = profileDoc.getString("neighbourhood") ?: "",
                    photoUrl = profileDoc.getString("photoUrl") ?: "",
                    email = profileDoc.getString("email") ?: auth.currentUser?.email ?: "",
                    role = profileDoc.getString("role") ?: "member",
                    skills = (profileDoc.get("skills") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )

                // Query only by authorId, sort in memory
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
                            Log.e(TAG, "Error parsing post", e)
                            null
                        }
                    }
                    .sortedByDescending { it.time }
                    .take(20)

                _uiState.value = ProfileUiState.Success(profile, posts)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile", e)
                _uiState.value = ProfileUiState.Error(
                    e.localizedMessage ?: "Failed to load profile"
                )
            }
        }
    }

    /* ==================== UPDATE PROFILE (Member) ==================== */

    fun updateProfile(
        name: String,
        bio: String,
        neighbourhood: String,
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
                            "neighbourhood" to neighbourhood
                        )
                    )
                    .await()

                loadProfile() // refresh UI
                _isUpdating.value = false
                onComplete()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                _isUpdating.value = false
                _uiState.value = ProfileUiState.Error(
                    e.localizedMessage ?: "Failed to update profile"
                )
            }
        }
    }

    /* ==================== UPDATE PROVIDER PROFILE ==================== */

    fun updateProviderProfile(
        name: String,
        bio: String,
        serviceType: String,
        skills: List<String>,
        isAvailable: Boolean,
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
                            "serviceType" to serviceType,
                            "skills" to skills,
                            "isAvailable" to isAvailable
                        )
                    )
                    .await()

                loadProfile() // refresh UI
                _isUpdating.value = false
                onComplete()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating provider profile", e)
                _isUpdating.value = false
                _uiState.value = ProfileUiState.Error(
                    e.localizedMessage ?: "Failed to update profile"
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

            userDoc.getString("neighbourhood")?.takeIf { it.isNotBlank() } ?: "Your Neighborhood"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting neighborhood", e)
            "Your Neighborhood"
        }
    }

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
                Log.e(TAG, "Error uploading image", e)
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
                Log.e(TAG, "Error deleting post", e)
            }
        }
    }

    /* ==================== LOAD USER PROFILE (For viewing others with POSTS) ==================== */

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Loading

                val doc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (!doc.exists()) {
                    _uiState.value = ProfileUiState.Error("User not found")
                    return@launch
                }

                val profile = UserProfile(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    name = doc.getString("name") ?: "",
                    role = doc.getString("role") ?: "member",
                    neighbourhood = doc.getString("neighbourhood") ?: "",
                    bio = doc.getString("bio") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    skills = (doc.get("skills") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )

                // Load posts for this user
                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .get()
                    .await()

                val posts = postsSnapshot.documents
                    .mapNotNull { postDoc ->
                        try {
                            val type = postDoc.getString("type") ?: return@mapNotNull null
                            if (type != "post") return@mapNotNull null

                            val timestamp = when (val createdAt = postDoc.get("createdAt")) {
                                is com.google.firebase.Timestamp -> createdAt.toDate().time
                                is Long -> createdAt
                                else -> System.currentTimeMillis()
                            }

                            FeedItem.Post(
                                id = postDoc.id,
                                author = postDoc.getString("authorName") ?: "",
                                authorId = postDoc.getString("authorId") ?: userId,
                                content = postDoc.getString("content") ?: "",
                                time = timestamp,
                                likes = (postDoc.getLong("likes") ?: 0L).toInt(),
                                commentCount = (postDoc.getLong("commentCount") ?: 0L).toInt(),
                                likedByMe = false,
                                imageUrl = postDoc.getString("imageUrl")
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing post", e)
                            null
                        }
                    }
                    .sortedByDescending { it.time }
                    .take(20)

                _uiState.value = ProfileUiState.Success(profile, posts)
                Log.d(TAG, "Loaded user profile for $userId with ${posts.size} posts")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }
}