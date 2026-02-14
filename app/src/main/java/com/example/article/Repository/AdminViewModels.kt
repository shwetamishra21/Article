package com.example.article.Repository

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

// ==================== DATA CLASSES ====================

data class Member(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val joinedDate: Long = 0L,
    val isActive: Boolean = true,
    val role: String = "member" // "member" or "admin"
)

data class Provider(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val skills: List<String> = emptyList(),
    val rating: Float? = null,
    val reviewCount: Int = 0,
    val status: String = "pending" // "pending", "approved", "rejected"
)

data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdDate: Long = 0L,
    val isPinned: Boolean = false
)

data class Post(
    val id: String = "",
    val authorName: String = "",
    val authorId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val reportCount: Int = 0,
    val isReported: Boolean = false
)

// ==================== VIEW MODELS ====================

class MemberManagementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadMembers(neighbourhoodId: String) {
        _loading.value = true
        _error.value = null

        try {
            val snapshot = db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("members")
                .get()
                .await()

            _members.value = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Member::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            _error.value = "Failed to load members: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun addMember(neighbourhoodId: String, name: String, email: String) {
        try {
            val member = Member(
                name = name,
                email = email,
                joinedDate = System.currentTimeMillis(),
                isActive = true,
                role = "member"
            )

            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("members")
                .add(member)
                .await()

            // Reload members
            loadMembers(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to add member: ${e.message}"
        }
    }

    suspend fun removeMember(neighbourhoodId: String, memberId: String) {
        try {
            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("members")
                .document(memberId)
                .delete()
                .await()

            // Reload members
            loadMembers(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to remove member: ${e.message}"
        }
    }
}

class ProviderApprovalViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _providers = MutableStateFlow<List<Provider>>(emptyList())
    val providers: StateFlow<List<Provider>> = _providers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadProviders(neighbourhoodId: String) {
        _loading.value = true
        _error.value = null

        try {
            val snapshot = db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("providers")
                .get()
                .await()

            _providers.value = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Provider::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            _error.value = "Failed to load providers: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun approveProvider(neighbourhoodId: String, providerId: String) {
        updateProviderStatus(neighbourhoodId, providerId, "approved")
    }

    suspend fun rejectProvider(neighbourhoodId: String, providerId: String) {
        updateProviderStatus(neighbourhoodId, providerId, "rejected")
    }

    suspend fun removeProvider(neighbourhoodId: String, providerId: String) {
        try {
            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("providers")
                .document(providerId)
                .delete()
                .await()

            loadProviders(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to remove provider: ${e.message}"
        }
    }

    private suspend fun updateProviderStatus(
        neighbourhoodId: String,
        providerId: String,
        status: String
    ) {
        try {
            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("providers")
                .document(providerId)
                .update("status", status)
                .await()

            loadProviders(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to update provider: ${e.message}"
        }
    }
}

class AnnouncementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadAnnouncements(neighbourhoodId: String) {
        _loading.value = true
        _error.value = null

        try {
            val snapshot = db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("announcements")
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            _announcements.value = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Announcement::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            _error.value = "Failed to load announcements: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun createAnnouncement(
        neighbourhoodId: String,
        title: String,
        content: String,
        createdBy: String,
        isPinned: Boolean
    ) {
        try {
            val announcement = Announcement(
                title = title,
                content = content,
                createdBy = createdBy,
                createdDate = System.currentTimeMillis(),
                isPinned = isPinned
            )

            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("announcements")
                .add(announcement)
                .await()

            loadAnnouncements(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to create announcement: ${e.message}"
        }
    }

    suspend fun togglePin(neighbourhoodId: String, announcementId: String, isPinned: Boolean) {
        try {
            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("announcements")
                .document(announcementId)
                .update("isPinned", !isPinned)
                .await()

            loadAnnouncements(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to update announcement: ${e.message}"
        }
    }

    suspend fun deleteAnnouncement(neighbourhoodId: String, announcementId: String) {
        try {
            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("announcements")
                .document(announcementId)
                .delete()
                .await()

            loadAnnouncements(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to delete announcement: ${e.message}"
        }
    }
}

class ContentModerationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadPosts(neighbourhoodId: String) {
        _loading.value = true
        _error.value = null

        try {
            val snapshot = db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("posts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            _posts.value = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            _error.value = "Failed to load posts: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun deletePost(neighbourhoodId: String, postId: String) {
        try {
            db.collection("neighbourhoods")
                .document(neighbourhoodId)
                .collection("posts")
                .document(postId)
                .delete()
                .await()

            loadPosts(neighbourhoodId)
        } catch (e: Exception) {
            _error.value = "Failed to delete post: ${e.message}"
        }
    }
}