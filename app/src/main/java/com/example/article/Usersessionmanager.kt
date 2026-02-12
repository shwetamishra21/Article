package com.example.article

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Singleton to manage user session and profile data across the app.
 *
 * This ensures:
 * - Profile is loaded once on app start
 * - Profile is accessible from any screen
 * - No redundant Firestore fetches when creating posts/comments
 */
object UserSessionManager {

    private const val TAG = "UserSessionManager"

    // StateFlow to hold current user profile
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Load user profile from Firestore
     * Call this after Firebase Auth succeeds
     */
    suspend fun loadUserProfile(uid: String, firestore: FirebaseFirestore): Result<UserProfile> {
        return try {
            _isLoading.value = true
            Log.d(TAG, "Loading profile for UID: $uid")

            val documentSnapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val profile = UserProfile(
                    uid = documentSnapshot.getString("uid") ?: uid,
                    email = documentSnapshot.getString("email") ?: "",
                    name = documentSnapshot.getString("name") ?: "",
                    role = documentSnapshot.getString("role") ?: "member",
                    neighbourhood = documentSnapshot.getString("neighbourhood") ?: "",
                    bio = documentSnapshot.getString("bio") ?: "",
                    photoUrl = documentSnapshot.getString("photoUrl") ?: "",
                    photoPublicId = documentSnapshot.getString("photoPublicId") ?: "",
                    createdAt = documentSnapshot.getTimestamp("createdAt") ?: Timestamp.now()
                )

                _currentUser.value = profile
                _isLoading.value = false

                Log.d(TAG, "Profile loaded successfully: ${profile.name}")
                Result.success(profile)
            } else {
                _isLoading.value = false
                Log.e(TAG, "Profile document does not exist for UID: $uid")
                Result.failure(Exception("Profile not found"))
            }

        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "Failed to load profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Refresh the current user profile
     * Call this after profile updates
     */
    suspend fun refreshProfile(firestore: FirebaseFirestore) {
        val currentUid = _currentUser.value?.uid ?: return
        loadUserProfile(currentUid, firestore)
    }

    /**
     * Clear session data on logout
     */
    fun clearSession() {
        Log.d(TAG, "Clearing user session")
        _currentUser.value = null
        _isLoading.value = false
    }

    /**
     * Check if user is logged in and profile is loaded
     */
    fun isUserLoggedIn(): Boolean {
        return _currentUser.value != null
    }

    /**
     * Get current user UID (shorthand)
     */
    fun getCurrentUid(): String? {
        return _currentUser.value?.uid
    }
}

/**
 * Data class representing user profile
 * Maps directly to Firestore /users/{uid} document
 */
data class UserProfile(
    val uid: String,
    val email: String,
    val name: String,
    val role: String,
    val neighbourhood: String,
    val bio: String,
    val photoUrl: String,
    val photoPublicId: String,
    val createdAt: Timestamp
)