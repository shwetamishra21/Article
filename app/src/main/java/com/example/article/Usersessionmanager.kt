package com.example.article

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

object UserSessionManager {

    private const val TAG = "UserSessionManager"

    // Stable scope not tied to any composition — survives navigation
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /* ---------------- CURRENT USER STATE ---------------- */

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    /* ---------------- LOADING STATE ---------------- */

    // IMPORTANT: start as TRUE so app shows loader on launch
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /* =====================================================
       SAFE TIMESTAMP HELPER (Fixes RuntimeException crash)
       ===================================================== */

    private fun DocumentSnapshot.getTimestampSafe(field: String): Timestamp {
        return try {
            getTimestamp(field) ?: Timestamp.now()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Invalid $field field type, using now: ${e.message}")
            Timestamp.now()
        }
    }

    /* =====================================================
       SHARED PROFILE BUILDER
       ===================================================== */

    private fun buildProfile(uid: String, doc: DocumentSnapshot) = UserProfile(
        uid = doc.getString("uid") ?: uid,
        email = doc.getString("email") ?: "",
        name = doc.getString("name") ?: "",
        role = doc.getString("role") ?: "member",
        neighbourhood = doc.getString("neighbourhood") ?: "",
        bio = doc.getString("bio") ?: "",
        photoUrl = doc.getString("photoUrl") ?: "",
        photoPublicId = doc.getString("photoPublicId") ?: "",
        createdAt = doc.getTimestampSafe("createdAt")
    )

    /* =====================================================
       LOAD PROFILE — used on login only, touches _isLoading
       ===================================================== */

    suspend fun loadUserProfile(
        uid: String,
        firestore: FirebaseFirestore
    ): Result<UserProfile> {

        return try {
            Log.d(TAG, "Loading profile for UID = $uid")
            _isLoading.value = true

            val doc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                Log.e(TAG, "Profile document missing for UID=$uid")
                _currentUser.value = null
                _isLoading.value = false
                return Result.failure(Exception("Profile not found"))
            }

            val profile = buildProfile(uid, doc)
            _currentUser.value = profile
            _isLoading.value = false

            Log.d(TAG, "Profile load SUCCESS → ${profile.email}")
            Result.success(profile)

        } catch (e: Exception) {
            Log.e(TAG, "Profile load FAILED", e)
            _currentUser.value = null
            _isLoading.value = false
            Result.failure(e)
        }
    }

    /* =====================================================
       REFRESH PROFILE — silent background update.
       NEVER touches _isLoading — cannot trigger loading
       screen or cause an infinite recomposition loop.
       ===================================================== */

    fun refreshProfile(firestore: FirebaseFirestore) {
        val uid = _currentUser.value?.uid ?: return
        Log.d(TAG, "Refreshing profile for $uid")
        scope.launch {
            try {
                val doc = firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()
                if (doc.exists()) {
                    _currentUser.value = buildProfile(uid, doc)
                    Log.d(TAG, "Profile refresh SUCCESS → ${_currentUser.value?.email}")
                }
            } catch (e: Exception) {
                // Silent — do NOT clear session or touch _isLoading
                Log.e(TAG, "Profile refresh FAILED (silent)", e)
            }
        }
    }

    /* =====================================================
       FORCE SESSION (NEW — FIXES LOGIN STUCK ISSUE)
       ===================================================== */

    fun setUser(profile: UserProfile) {
        Log.d(TAG, "Session manually set for ${profile.email}")
        _currentUser.value = profile
        _isLoading.value = false
    }

    /* =====================================================
       LOGOUT
       ===================================================== */

    fun clearSession() {
        Log.d(TAG, "Session cleared")
        _currentUser.value = null
        _isLoading.value = false
    }

    /* =====================================================
       HELPERS
       ===================================================== */

    fun isUserLoggedIn(): Boolean = _currentUser.value != null

    fun getCurrentUid(): String? = _currentUser.value?.uid
}

/* =========================================================
   DATA MODEL
   ========================================================= */

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