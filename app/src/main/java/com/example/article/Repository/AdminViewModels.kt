package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

// ==================== DATA CLASSES ====================

data class AdminMember(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val joinedDate: Long = 0L,
    val isActive: Boolean = true,
    val role: String = "member"
)

data class AdminProvider(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val skills: List<String> = emptyList(),
    val rating: Float? = null,
    val reviewCount: Int = 0,
    val status: String = "pending",
    val userId: String = ""
)

data class AdminAnnouncement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdDate: Long = 0L,
    val isPinned: Boolean = false
)

data class AdminPost(
    val id: String = "",
    val authorName: String = "",
    val authorId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val reportCount: Int = 0,
    val isReported: Boolean = false
)

// ==================== MEMBER MANAGEMENT VIEW MODEL ====================

class MemberManagementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _members = MutableStateFlow<List<AdminMember>>(emptyList())
    val members: StateFlow<List<AdminMember>> = _members

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadMembers() {
        _loading.value = true
        _error.value = null

        try {
            Log.d("MemberManagementVM", "Loading members...")
            val snapshot = db.collection("users")
                .whereIn("role", listOf("member", "admin"))
                .get()
                .await()

            _members.value = snapshot.documents.mapNotNull { doc ->
                try {
                    AdminMember(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        joinedDate = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        isActive = true,
                        role = doc.getString("role") ?: "member"
                    )
                } catch (e: Exception) {
                    Log.e("MemberManagementVM", "Error parsing member: ${e.message}")
                    null
                }
            }
            Log.d("MemberManagementVM", "Loaded ${_members.value.size} members")
        } catch (e: Exception) {
            Log.e("MemberManagementVM", "Failed to load members", e)
            _error.value = "Failed to load members: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun addMember(name: String, email: String): Result<Unit> {
        return try {
            Log.d("MemberManagementVM", "Adding member: $email")

            val userId = db.collection("users").document().id
            val member = hashMapOf(
                "uid" to userId,
                "name" to name,
                "email" to email,
                "role" to "member",
                "neighbourhood" to "",
                "bio" to "",
                "photoUrl" to "",
                "photoPublicId" to "",
                "createdAt" to Timestamp.now()
            )

            db.collection("users")
                .document(userId)
                .set(member)
                .await()

            Log.d("MemberManagementVM", "Member added successfully")
            loadMembers()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MemberManagementVM", "Failed to add member", e)
            _error.value = "Failed to add member: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun removeMember(memberId: String): Result<Unit> {
        return try {
            Log.d("MemberManagementVM", "Removing member: $memberId")
            db.collection("users")
                .document(memberId)
                .delete()
                .await()

            Log.d("MemberManagementVM", "Member removed successfully")
            loadMembers()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MemberManagementVM", "Failed to remove member", e)
            _error.value = "Failed to remove member: ${e.message}"
            Result.failure(e)
        }
    }
}

// ==================== PROVIDER APPROVAL VIEW MODEL ====================

class ProviderApprovalViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _providers = MutableStateFlow<List<AdminProvider>>(emptyList())
    val providers: StateFlow<List<AdminProvider>> = _providers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadProviders() {
        _loading.value = true
        _error.value = null

        try {
            Log.d("ProviderApprovalVM", "Loading providers...")
            val snapshot = db.collection("users")
                .whereEqualTo("role", "service_provider")
                .get()
                .await()

            _providers.value = snapshot.documents.mapNotNull { doc ->
                try {
                    AdminProvider(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("serviceType") ?: "General",
                        skills = (doc.get("skills") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        rating = doc.getDouble("rating")?.toFloat(),
                        reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                        status = doc.getString("status") ?: "pending",
                        userId = doc.id
                    )
                } catch (e: Exception) {
                    Log.e("ProviderApprovalVM", "Error parsing provider: ${e.message}")
                    null
                }
            }
            Log.d("ProviderApprovalVM", "Loaded ${_providers.value.size} providers")
        } catch (e: Exception) {
            Log.e("ProviderApprovalVM", "Failed to load providers", e)
            _error.value = "Failed to load providers: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun approveProvider(providerId: String): Result<Unit> {
        return updateProviderStatus(providerId, "approved")
    }

    suspend fun rejectProvider(providerId: String): Result<Unit> {
        return updateProviderStatus(providerId, "rejected")
    }

    suspend fun removeProvider(providerId: String): Result<Unit> {
        return try {
            Log.d("ProviderApprovalVM", "Removing provider: $providerId")
            db.collection("users")
                .document(providerId)
                .delete()
                .await()

            Log.d("ProviderApprovalVM", "Provider removed successfully")
            loadProviders()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProviderApprovalVM", "Failed to remove provider", e)
            _error.value = "Failed to remove provider: ${e.message}"
            Result.failure(e)
        }
    }

    private suspend fun updateProviderStatus(providerId: String, status: String): Result<Unit> {
        return try {
            Log.d("ProviderApprovalVM", "Updating provider $providerId status to $status")
            db.collection("users")
                .document(providerId)
                .update("status", status)
                .await()

            Log.d("ProviderApprovalVM", "Provider status updated successfully")
            loadProviders()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProviderApprovalVM", "Failed to update provider", e)
            _error.value = "Failed to update provider: ${e.message}"
            Result.failure(e)
        }
    }
}

// ==================== ANNOUNCEMENT VIEW MODEL ====================

class AnnouncementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _announcements = MutableStateFlow<List<AdminAnnouncement>>(emptyList())
    val announcements: StateFlow<List<AdminAnnouncement>> = _announcements

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadAnnouncements() {
        _loading.value = true
        _error.value = null

        try {
            Log.d("AnnouncementVM", "Loading announcements...")
            val snapshot = db.collection("announcements")
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            _announcements.value = snapshot.documents.mapNotNull { doc ->
                try {
                    AdminAnnouncement(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdDate = doc.getLong("createdDate") ?: 0L,
                        isPinned = doc.getBoolean("isPinned") ?: false
                    )
                } catch (e: Exception) {
                    Log.e("AnnouncementVM", "Error parsing announcement: ${e.message}")
                    null
                }
            }
            Log.d("AnnouncementVM", "Loaded ${_announcements.value.size} announcements")
        } catch (e: Exception) {
            Log.e("AnnouncementVM", "Failed to load announcements", e)
            _error.value = "Failed to load announcements: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun createAnnouncement(
        title: String,
        content: String,
        createdBy: String,
        isPinned: Boolean
    ): Result<Unit> {
        return try {
            Log.d("AnnouncementVM", "Creating announcement: $title")
            val announcement = hashMapOf(
                "title" to title,
                "content" to content,
                "createdBy" to createdBy,
                "createdDate" to System.currentTimeMillis(),
                "isPinned" to isPinned
            )

            db.collection("announcements")
                .add(announcement)
                .await()

            Log.d("AnnouncementVM", "Announcement created successfully")
            loadAnnouncements()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AnnouncementVM", "Failed to create announcement", e)
            _error.value = "Failed to create announcement: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun togglePin(announcementId: String, currentPinState: Boolean): Result<Unit> {
        return try {
            Log.d("AnnouncementVM", "Toggling pin for announcement: $announcementId")
            db.collection("announcements")
                .document(announcementId)
                .update("isPinned", !currentPinState)
                .await()

            Log.d("AnnouncementVM", "Pin toggled successfully")
            loadAnnouncements()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AnnouncementVM", "Failed to toggle pin", e)
            _error.value = "Failed to update announcement: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        return try {
            Log.d("AnnouncementVM", "Deleting announcement: $announcementId")
            db.collection("announcements")
                .document(announcementId)
                .delete()
                .await()

            Log.d("AnnouncementVM", "Announcement deleted successfully")
            loadAnnouncements()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AnnouncementVM", "Failed to delete announcement", e)
            _error.value = "Failed to delete announcement: ${e.message}"
            Result.failure(e)
        }
    }
}

// ==================== CONTENT MODERATION VIEW MODEL ====================

class ContentModerationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _posts = MutableStateFlow<List<AdminPost>>(emptyList())
    val posts: StateFlow<List<AdminPost>> = _posts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadPosts() {
        _loading.value = true
        _error.value = null

        try {
            Log.d("ContentModerationVM", "Loading posts...")
            val snapshot = db.collection("posts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            _posts.value = snapshot.documents.mapNotNull { doc ->
                try {
                    AdminPost(
                        id = doc.id,
                        authorName = doc.getString("authorName") ?: "Unknown",
                        authorId = doc.getString("authorId") ?: "",
                        content = doc.getString("content") ?: "",
                        imageUrl = doc.getString("imageUrl"),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        reportCount = doc.getLong("reportCount")?.toInt() ?: 0,
                        isReported = (doc.getLong("reportCount")?.toInt() ?: 0) > 0
                    )
                } catch (e: Exception) {
                    Log.e("ContentModerationVM", "Error parsing post: ${e.message}")
                    null
                }
            }
            Log.d("ContentModerationVM", "Loaded ${_posts.value.size} posts")
        } catch (e: Exception) {
            Log.e("ContentModerationVM", "Failed to load posts", e)
            _error.value = "Failed to load posts: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            Log.d("ContentModerationVM", "Deleting post: $postId")
            db.collection("posts")
                .document(postId)
                .delete()
                .await()

            Log.d("ContentModerationVM", "Post deleted successfully")
            loadPosts()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ContentModerationVM", "Failed to delete post", e)
            _error.value = "Failed to delete post: ${e.message}"
            Result.failure(e)
        }
    }
}