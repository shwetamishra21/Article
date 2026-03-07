package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ==================== DATA CLASSES ====================

data class AdminMember(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val joinedDate: Long = 0L,
    val isActive: Boolean = true,
    val role: String = "member",
    val isBanned: Boolean = false,
    val neighbourhoodId: String = ""
)

data class AdminProvider(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val category: String = "",
    val skills: List<String> = emptyList(),
    val rating: Float? = null,
    val reviewCount: Int = 0,
    val status: String = "pending" // "pending" | "approved" | "rejected"
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
    private val auth = FirebaseAuth.getInstance()

    private val _members = MutableStateFlow<List<AdminMember>>(emptyList())
    val members: StateFlow<List<AdminMember>> = _members.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val isLoading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
        _error.value = null
    }

    private suspend fun getAdminNeighbourhoodId(): String? {
        val adminUid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminUid)
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                Log.d("MemberManagementVM", "Loading members...")
                val nId = getAdminNeighbourhoodId()

                val snapshot = if (nId != null) {
                    db.collection("users")
                        .whereEqualTo("role", "member")
                        .whereEqualTo("neighbourhoodId", nId)
                        .get()
                        .await()
                } else {
                    db.collection("users")
                        .whereIn("role", listOf("member", "admin"))
                        .get()
                        .await()
                }

                _members.value = snapshot.documents.mapNotNull { doc ->
                    try {
                        AdminMember(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            joinedDate = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            isActive = !(doc.getBoolean("isBanned") ?: false),
                            role = doc.getString("role") ?: "member",
                            isBanned = doc.getBoolean("isBanned") ?: false,
                            neighbourhoodId = doc.getString("neighbourhoodId") ?: ""
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
    }

    fun toggleBan(member: AdminMember) {
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(member.id)
                    .update("isBanned", !member.isBanned)
                    .await()
                _message.value = if (member.isBanned) "${member.name} unbanned" else "${member.name} banned"
                loadMembers()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update member"
            }
        }
    }

    fun removeMember(member: AdminMember) {
        viewModelScope.launch {
            try {
                Log.d("MemberManagementVM", "Removing member: ${member.id}")
                val nId = getAdminNeighbourhoodId()

                db.collection("users")
                    .document(member.id)
                    .update("neighbourhoodId", null)
                    .await()

                if (nId != null) {
                    val requestsSnap = db.collection("join_requests")
                        .whereEqualTo("userId", member.id)
                        .whereEqualTo("neighbourhoodId", nId)
                        .whereEqualTo("status", "approved")
                        .get()
                        .await()
                    for (doc in requestsSnap.documents) {
                        doc.reference.update("status", "removed").await()
                    }

                    val neighRef = db.collection("neighbourhoods").document(nId)
                    val neighDoc = neighRef.get().await()
                    val currentCount = neighDoc.getLong("memberCount") ?: 1L
                    neighRef.update("memberCount", maxOf(0L, currentCount - 1)).await()
                }

                _message.value = "${member.name} removed from neighbourhood"
                Log.d("MemberManagementVM", "Member removed successfully")
                loadMembers()
            } catch (e: Exception) {
                Log.e("MemberManagementVM", "Failed to remove member", e)
                _error.value = "Failed to remove member: ${e.message}"
            }
        }
    }
}

// ==================== PROVIDER APPROVAL VIEW MODEL ====================

class ProviderApprovalViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _providers = MutableStateFlow<List<AdminProvider>>(emptyList())
    val providers: StateFlow<List<AdminProvider>> = _providers.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
        _error.value = null
    }

    private suspend fun getAdminNeighbourhoodId(): String? {
        val adminUid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminUid)
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    fun loadProviders() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                Log.d("ProviderApprovalVM", "Loading providers...")
                val nId = getAdminNeighbourhoodId()
                    ?: run {
                        _providers.value = emptyList()
                        return@launch
                    }

                val requestsSnap = db.collection("join_requests")
                    .whereEqualTo("neighbourhoodId", nId)
                    .whereEqualTo("userRole", "service_provider")
                    .get()
                    .await()

                val result = mutableListOf<AdminProvider>()
                for (doc in requestsSnap.documents) {
                    val status = doc.getString("status") ?: "pending"
                    if (status == "removed") continue

                    val userId = doc.getString("userId") ?: continue
                    val userDoc = try {
                        db.collection("users").document(userId).get().await()
                    } catch (e: Exception) {
                        null
                    }

                    result.add(
                        AdminProvider(
                            id = doc.id,
                            userId = userId,
                            name = doc.getString("userName")
                                ?: userDoc?.getString("name") ?: "Unknown",
                            email = doc.getString("userEmail")
                                ?: userDoc?.getString("email") ?: "",
                            category = userDoc?.getString("serviceType")
                                ?: userDoc?.getString("category") ?: "General",
                            skills = (userDoc?.get("skills") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            rating = userDoc?.getDouble("rating")?.toFloat(),
                            reviewCount = (userDoc?.getLong("reviewCount") ?: 0L).toInt(),
                            status = status
                        )
                    )
                }

                _providers.value = result
                Log.d("ProviderApprovalVM", "Loaded ${result.size} providers")
            } catch (e: Exception) {
                Log.e("ProviderApprovalVM", "Failed to load providers", e)
                _error.value = "Failed to load providers: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun approveProvider(requestId: String) {
        viewModelScope.launch {
            try {
                val provider = _providers.value.find { it.id == requestId }
                db.collection("join_requests")
                    .document(requestId)
                    .update("status", "approved")
                    .await()

                val nId = getAdminNeighbourhoodId()
                if (nId != null) {
                    val neighRef = db.collection("neighbourhoods").document(nId)
                    val count = neighRef.get().await().getLong("providerCount") ?: 0L
                    neighRef.update("providerCount", count + 1).await()
                }

                _message.value = "${provider?.name ?: "Provider"} approved"
                loadProviders()
            } catch (e: Exception) {
                Log.e("ProviderApprovalVM", "Failed to approve provider", e)
                _error.value = "Failed to approve provider: ${e.message}"
            }
        }
    }

    fun rejectProvider(requestId: String) {
        viewModelScope.launch {
            try {
                val provider = _providers.value.find { it.id == requestId }
                db.collection("join_requests")
                    .document(requestId)
                    .update("status", "rejected")
                    .await()

                _message.value = "${provider?.name ?: "Provider"}'s request rejected"
                loadProviders()
            } catch (e: Exception) {
                Log.e("ProviderApprovalVM", "Failed to reject provider", e)
                _error.value = "Failed to reject provider: ${e.message}"
            }
        }
    }

    fun removeProvider(requestId: String) {
        viewModelScope.launch {
            try {
                val provider = _providers.value.find { it.id == requestId }
                db.collection("join_requests")
                    .document(requestId)
                    .update("status", "removed")
                    .await()

                val nId = getAdminNeighbourhoodId()
                if (nId != null) {
                    val neighRef = db.collection("neighbourhoods").document(nId)
                    val count = neighRef.get().await().getLong("providerCount") ?: 1L
                    neighRef.update("providerCount", maxOf(0L, count - 1)).await()
                }

                _message.value = "${provider?.name ?: "Provider"} removed"
                loadProviders()
            } catch (e: Exception) {
                Log.e("ProviderApprovalVM", "Failed to remove provider", e)
                _error.value = "Failed to remove provider: ${e.message}"
            }
        }
    }
}

// ==================== ANNOUNCEMENT VIEW MODEL ====================

class AnnouncementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _announcements = MutableStateFlow<List<AdminAnnouncement>>(emptyList())
    val announcements: StateFlow<List<AdminAnnouncement>> = _announcements.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
        _error.value = null
    }

    private suspend fun getAdminNeighbourhoodId(): String? {
        val adminUid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminUid)
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                Log.d("AnnouncementVM", "Loading announcements...")
                val nId = getAdminNeighbourhoodId()
                    ?: run {
                        _announcements.value = emptyList()
                        return@launch
                    }

                val snapshot = db.collection("neighbourhoods")
                    .document(nId)
                    .collection("announcements")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                _announcements.value = snapshot.documents.mapNotNull { doc ->
                    try {
                        AdminAnnouncement(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            createdDate = doc.getLong("createdAt") ?: 0L,
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
    }

    fun createAnnouncement(title: String, content: String, isPinned: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val createdBy = auth.currentUser?.uid ?: ""
                val nId = getAdminNeighbourhoodId()
                    ?: throw Exception("No neighbourhood found")

                Log.d("AnnouncementVM", "Creating announcement: $title")
                val data = mapOf(
                    "title" to title,
                    "content" to content,
                    "createdBy" to createdBy,
                    "createdAt" to System.currentTimeMillis(),
                    "isPinned" to isPinned,
                    "neighbourhoodId" to nId
                )
                db.collection("neighbourhoods")
                    .document(nId)
                    .collection("announcements")
                    .add(data)
                    .await()
                _message.value = "Announcement created"
                Log.d("AnnouncementVM", "Announcement created successfully")
                loadAnnouncements()
            } catch (e: Exception) {
                Log.e("AnnouncementVM", "Failed to create announcement", e)
                _error.value = "Failed to create announcement: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun togglePin(announcement: AdminAnnouncement) {
        viewModelScope.launch {
            try {
                Log.d("AnnouncementVM", "Toggling pin for: ${announcement.id}")
                val nId = getAdminNeighbourhoodId() ?: return@launch
                db.collection("neighbourhoods")
                    .document(nId)
                    .collection("announcements")
                    .document(announcement.id)
                    .update("isPinned", !announcement.isPinned)
                    .await()
                _message.value = if (!announcement.isPinned) "Pinned" else "Unpinned"
                Log.d("AnnouncementVM", "Pin toggled successfully")
                loadAnnouncements()
            } catch (e: Exception) {
                Log.e("AnnouncementVM", "Failed to toggle pin", e)
                _error.value = "Failed to update announcement: ${e.message}"
            }
        }
    }

    fun deleteAnnouncement(announcement: AdminAnnouncement) {
        viewModelScope.launch {
            try {
                Log.d("AnnouncementVM", "Deleting announcement: ${announcement.id}")
                val nId = getAdminNeighbourhoodId() ?: return@launch
                db.collection("neighbourhoods")
                    .document(nId)
                    .collection("announcements")
                    .document(announcement.id)
                    .delete()
                    .await()
                _message.value = "Announcement deleted"
                Log.d("AnnouncementVM", "Announcement deleted successfully")
                loadAnnouncements()
            } catch (e: Exception) {
                Log.e("AnnouncementVM", "Failed to delete announcement", e)
                _error.value = "Failed to delete announcement: ${e.message}"
            }
        }
    }
}

// ==================== CONTENT MODERATION VIEW MODEL ====================

class ContentModerationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = MutableStateFlow<List<AdminPost>>(emptyList())
    val posts: StateFlow<List<AdminPost>> = _posts.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
        _error.value = null
    }

    private suspend fun getAdminNeighbourhoodId(): String? {
        val adminUid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminUid)
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                Log.d("ContentModerationVM", "Loading posts...")
                val nId = getAdminNeighbourhoodId()
                    ?: run {
                        _posts.value = emptyList()
                        return@launch
                    }

                val query = db.collection("posts")
                    .whereEqualTo("neighbourhoodId", nId)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                _posts.value = query.documents.mapNotNull { doc ->
                    try {
                        val reportCount = doc.getLong("reportCount")?.toInt() ?: 0
                        AdminPost(
                            id = doc.id,
                            authorName = doc.getString("authorName") ?: "Unknown",
                            authorId = doc.getString("authorId") ?: "",
                            content = doc.getString("content") ?: return@mapNotNull null,
                            imageUrl = doc.getString("imageUrl"),
                            timestamp = doc.getLong("createdAt") ?: 0L,
                            reportCount = reportCount,
                            isReported = reportCount > 0
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
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                Log.d("ContentModerationVM", "Deleting post: $postId")
                db.collection("posts").document(postId).delete().await()
                _message.value = "Post removed"
                _posts.value = _posts.value.filter { it.id != postId }
                Log.d("ContentModerationVM", "Post deleted successfully")
            } catch (e: Exception) {
                Log.e("ContentModerationVM", "Failed to delete post", e)
                _error.value = "Failed to delete post: ${e.message}"
            }
        }
    }

    fun dismissReport(post: AdminPost) {
        viewModelScope.launch {
            try {
                db.collection("posts").document(post.id).update("reportCount", 0).await()
                _message.value = "Report dismissed"
                _posts.value = _posts.value.map {
                    if (it.id == post.id) it.copy(reportCount = 0, isReported = false) else it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to dismiss report"
            }
        }
    }
}