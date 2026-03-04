package com.example.article.Repository

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.FeedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
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
    val skills: List<String> = emptyList(),
    // ── Provider-specific (safe defaults for members) ──
    val serviceType: String = "",
    val isAvailable: Boolean = true,
    val averageRating: Float = 0f,
    val ratingCount: Int = 0
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

    private val _activeRequests = MutableStateFlow(0)
    val activeRequests: StateFlow<Int> = _activeRequests

    private val _completedJobs = MutableStateFlow(0)
    val completedJobs: StateFlow<Int> = _completedJobs

    private var activeListener: ListenerRegistration? = null
    private var completedListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    // ── Shared helper to avoid duplicating parsing logic ──────────────────────
    private fun parseProfile(uid: String, doc: com.google.firebase.firestore.DocumentSnapshot) = UserProfile(
        uid = uid,
        name = doc.getString("name") ?: "",
        bio = doc.getString("bio") ?: "",
        neighbourhood = doc.getString("neighbourhood") ?: "",
        photoUrl = doc.getString("photoUrl") ?: "",
        email = doc.getString("email") ?: auth.currentUser?.email ?: "",
        role = doc.getString("role") ?: "member",
        skills = (doc.get("skills") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        serviceType = doc.getString("serviceType") ?: "",
        isAvailable = doc.getBoolean("isAvailable") ?: true,
        averageRating = (doc.getDouble("averageRating") ?: 0.0).toFloat(),
        ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt()
    )

    private fun parsePosts(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        fallbackUserId: String
    ): List<FeedItem.Post> = snapshot.documents.mapNotNull { doc ->
        try {
            if (doc.getString("type") != "post") return@mapNotNull null
            val timestamp = when (val createdAt = doc.get("createdAt")) {
                is com.google.firebase.Timestamp -> createdAt.toDate().time
                is Long -> createdAt
                else -> System.currentTimeMillis()
            }
            FeedItem.Post(
                id = doc.id,
                author = doc.getString("authorName") ?: "",
                authorId = doc.getString("authorId") ?: fallbackUserId,
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
    }.sortedByDescending { it.time }.take(20)

    /* ==================== LOAD PROFILE (Own Profile) ==================== */

    fun loadProfile() {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = ProfileUiState.Error("Not authenticated")
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Loading
                val profileDoc = firestore.collection("users").document(userId).get().await()
                val profile = parseProfile(userId, profileDoc)
                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("authorId", userId).get().await()
                _uiState.value = ProfileUiState.Success(profile, parsePosts(postsSnapshot, userId))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile", e)
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Failed to load profile")
            }
        }
    }

    /* ==================== REAL-TIME PROVIDER STATS ==================== */

    fun listenToProviderStats(userId: String) {
        // Remove any existing listeners before attaching new ones
        activeListener?.remove()
        completedListener?.remove()

        activeListener = firestore.collection("service_requests")
            .whereEqualTo("providerId", userId)
            .whereIn("status", listOf("accepted", "in_progress"))
            .addSnapshotListener { snapshot, _ ->
                _activeRequests.value = snapshot?.size() ?: 0
            }

        completedListener = firestore.collection("service_requests")
            .whereEqualTo("providerId", userId)
            .whereEqualTo("status", "completed")
            .addSnapshotListener { snapshot, _ ->
                _completedJobs.value = snapshot?.size() ?: 0
            }
    }

    override fun onCleared() {
        super.onCleared()
        activeListener?.remove()
        completedListener?.remove()
    }

    /* ==================== UPDATE PROFILE (Member) ==================== */

    fun updateProfile(name: String, bio: String, neighbourhood: String, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isUpdating.value = true
                firestore.collection("users").document(userId)
                    .update(mapOf("name" to name, "bio" to bio, "neighbourhood" to neighbourhood))
                    .await()
                loadProfile()
                _isUpdating.value = false
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                _isUpdating.value = false
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Failed to update profile")
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
                firestore.collection("users").document(userId)
                    .update(mapOf(
                        "name" to name,
                        "bio" to bio,
                        "serviceType" to serviceType,
                        "skills" to skills,
                        "isAvailable" to isAvailable
                    )).await()
                loadProfile()
                _isUpdating.value = false
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating provider profile", e)
                _isUpdating.value = false
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    /* ==================== GET NEIGHBORHOOD (Helper) ==================== */

    suspend fun getUserNeighborhood(): String {
        return try {
            val userId = auth.currentUser?.uid ?: return "Your Neighborhood"
            firestore.collection("users").document(userId).get().await()
                .getString("neighbourhood")?.takeIf { it.isNotBlank() } ?: "Your Neighborhood"
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
                val uploadResult = CloudinaryHelper.uploadImage(uri, context, "profiles/$userId")
                if (uploadResult.isFailure) {
                    _isUpdating.value = false
                    _uiState.value = ProfileUiState.Error(uploadResult.exceptionOrNull()?.message ?: "Upload failed")
                    return@launch
                }
                val result = uploadResult.getOrNull()!!
                firestore.collection("users").document(userId)
                    .update(mapOf("photoUrl" to result.secureUrl, "photoPublicId" to result.publicId))
                    .await()
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(
                        profile = currentState.profile.copy(photoUrl = result.secureUrl)
                    )
                }
                _isUpdating.value = false
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                _isUpdating.value = false
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Failed to upload image")
            }
        }
    }

    /* ==================== DELETE POST ==================== */

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts").document(postId).delete().await()
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
                val doc = firestore.collection("users").document(userId).get().await()
                if (!doc.exists()) {
                    _uiState.value = ProfileUiState.Error("User not found")
                    return@launch
                }
                val profile = parseProfile(doc.id, doc)
                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("authorId", userId).get().await()
                _uiState.value = ProfileUiState.Success(profile, parsePosts(postsSnapshot, userId))
                Log.d(TAG, "Loaded user profile for $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    /* ==================== GET USER INFO (Helper for Chat) ==================== */

    suspend fun getUserInfo(userId: String): Pair<String, String>? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) Pair(userId, doc.getString("name") ?: "User") else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user info", e)
            null
        }
    }
}