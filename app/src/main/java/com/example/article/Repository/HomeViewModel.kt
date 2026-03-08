package com.example.article.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.FeedItem
import com.example.article.Repository.FeedRepository
import com.example.article.Repository.NeighbourhoodRepository
import com.example.article.UserSessionManager
import com.example.article.core.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.example.article.Repository.LikeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.article.CommentRepository

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = FeedRepository

    private val _uiState =
        MutableStateFlow<UiState<List<FeedItem>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<FeedItem>>> = _uiState

    private var lastVisible: DocumentSnapshot? = null
    private var listener: ListenerRegistration? = null

    private val PAGE_SIZE = 20

    fun loadFeed() {
        if (listener != null) return

        _uiState.value = UiState.Loading

        listener = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    _uiState.value = UiState.Error(error.message ?: "Firestore error")
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    _uiState.value = UiState.Success(emptyList())
                    lastVisible = null
                    return@addSnapshotListener
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    try { mapDoc(doc) } catch (_: Exception) { null }
                }

                lastVisible = snapshot.documents.lastOrNull()

                // Merge with any announcements already in state so they aren't wiped
                val existing = (_uiState.value as? UiState.Success)?.data ?: emptyList()
                val existingAnnouncements = existing.filterIsInstance<FeedItem.Announcement>()
                val merged = mergeAndSort(items, existingAnnouncements)
                _uiState.value = UiState.Success(merged)
            }
    }

    /**
     * Fetch announcements for the current user's neighbourhood.
     * Only members/admins of that neighbourhood will see them.
     * Call this after loadFeed() — typically in a LaunchedEffect in HomeScreen.
     */
    fun loadNeighbourhoodAnnouncements() {
        viewModelScope.launch {
            try {
                val neighbourhoodId = UserSessionManager.currentUser.value?.neighbourhoodId
                    ?.takeIf { it.isNotBlank() } ?: return@launch

                val snapshot = firestore
                    .collection("neighbourhoods")
                    .document(neighbourhoodId)
                    .collection("announcements")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val announcements = snapshot.documents.mapNotNull { doc ->
                    try {
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        FeedItem.Announcement(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("content") ?: "",
                            time = createdAt
                        )
                    } catch (_: Exception) { null }
                }

                // Merge announcements into the current feed
                val currentPosts = (_uiState.value as? UiState.Success)?.data
                    ?.filterIsInstance<FeedItem.Post>() ?: emptyList()
                _uiState.value = UiState.Success(mergeAndSort(currentPosts, announcements))

            } catch (_: Exception) {
                // Silent — posts still show even if announcements fail
            }
        }
    }

    /**
     * Merge posts and announcements, sorted newest-first.
     * Announcements always appear at the top (time = Long.MAX_VALUE trick avoided —
     * instead we sort by time but announcements use their real createdAt).
     */
    private fun mergeAndSort(
        posts: List<FeedItem>,
        announcements: List<FeedItem.Announcement>
    ): List<FeedItem> {
        // Deduplicate posts by id
        val dedupedPosts = posts.filterIsInstance<FeedItem.Post>().distinctBy { it.id }
        // Deduplicate announcements by id
        val dedupedAnnouncements = announcements.distinctBy { it.id }

        // Announcements first (pinned at top), then posts sorted by time
        return dedupedAnnouncements + dedupedPosts.sortedByDescending { it.time }
    }

    fun loadMore() {
        val last = lastVisible ?: return
        val current = (_uiState.value as? UiState.Success)?.data ?: return

        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .startAfter(last)
            .limit(PAGE_SIZE.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener

                lastVisible = snapshot.documents.last()

                val more = snapshot.documents.mapNotNull { mapDoc(it) }

                val merged = (current + more).distinctBy {
                    when (it) {
                        is FeedItem.Post -> it.id
                        is FeedItem.Announcement -> it.id
                    }
                }

                _uiState.value = UiState.Success(merged)
            }
    }

    fun toggleLikeOptimistic(postId: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        val updated = current.map {
            if (it is FeedItem.Post && it.id == postId) {
                it.copy(
                    likes = if (it.likedByMe) it.likes - 1 else it.likes + 1,
                    likedByMe = !it.likedByMe
                )
            } else it
        }
        _uiState.value = UiState.Success(updated)
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        toggleLikeOptimistic(postId)
        LikeRepository.toggleLike(
            postId = postId,
            isCurrentlyLiked = currentlyLiked,
            onSuccess = {},
            onError = { toggleLikeOptimistic(postId) }
        )
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            listener?.remove()
            listener = null

            repository.deletePost(postId)

            val current = (_uiState.value as? UiState.Success)?.data ?: emptyList()
            _uiState.value = UiState.Success(
                current.filterNot { it is FeedItem.Post && it.id == postId }
            )

            loadFeed()
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            listener?.remove()
            listener = null

            repository.deleteAnnouncement(announcementId)

            val current = (_uiState.value as? UiState.Success)?.data ?: emptyList()
            _uiState.value = UiState.Success(
                current.filterNot { it is FeedItem.Announcement && it.id == announcementId }
            )

            loadFeed()
        }
    }

    fun refreshFeed() {
        listener?.remove()
        listener = null
        lastVisible = null
        loadFeed()
        loadNeighbourhoodAnnouncements()
    }

    fun addComment(postId: String, authorId: String, author: String, text: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        val updated = current.map {
            if (it is FeedItem.Post && it.id == postId) {
                it.copy(commentCount = it.commentCount + 1)
            } else it
        }
        _uiState.value = UiState.Success(updated)

        CommentRepository.addComment(
            postId = postId,
            authorId = authorId,
            authorName = author,
            authorPhotoUrl = "",
            content = text,
            onComplete = {},
            onError = {
                val rollback = current.map {
                    if (it is FeedItem.Post && it.id == postId) {
                        it.copy(commentCount = it.commentCount - 1)
                    } else it
                }
                _uiState.value = UiState.Success(rollback)
            }
        )
    }

    private fun mapDoc(doc: DocumentSnapshot): FeedItem? {
        val type = doc.getString("type") ?: return null
        val createdAt = doc.getTimestamp("createdAt")
        val time = createdAt?.toDate()?.time ?: 0L
        val currentUid = auth.currentUser?.uid

        return when (type) {
            "post" -> {
                val likedBy = doc.get("likedBy") as? Map<*, *> ?: emptyMap<Any, Any>()
                val likedByMe = currentUid != null && likedBy.containsKey(currentUid)

                FeedItem.Post(
                    id = doc.id,
                    author = doc.getString("authorName") ?: "User",
                    authorId = doc.getString("authorId") ?: "",
                    content = doc.getString("content") ?: "",
                    time = time,
                    likes = (doc.getLong("likes") ?: 0L).toInt(),
                    commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                    likedByMe = likedByMe,
                    imageUrl = doc.getString("imageUrl"),
                    authorPhotoUrl = doc.getString("authorPhotoUrl")
                )
            }

            // "announcement" type in the global posts collection is intentionally
            // ignored here — announcements now come exclusively from
            // neighbourhoods/{id}/announcements via loadNeighbourhoodAnnouncements()
            else -> null
        }
    }

    fun addOptimistic(item: FeedItem) {
        val current = (_uiState.value as? UiState.Success)?.data ?: emptyList()
        _uiState.value = UiState.Success(listOf(item) + current)
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}