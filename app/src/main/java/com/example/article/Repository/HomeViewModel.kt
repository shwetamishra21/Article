package com.example.article.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.article.FeedItem
import com.example.article.Repository.FeedRepository
import com.example.article.core.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java. security. Timestamp

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

    /* ---------- LOAD FEED ---------- */

    fun loadFeed() {
        if (listener != null) return

        _uiState.value = UiState.Loading

        listener = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    _uiState.value =
                        UiState.Error(error.message ?: "Firestore error")
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    _uiState.value = UiState.Error("No data")
                    return@addSnapshotListener
                }

                val items = snapshot.documents.mapNotNull { mapDoc(it) }

                lastVisible = snapshot.documents.lastOrNull()

                _uiState.value = UiState.Success(items)
            }
    }

    /* ---------- PAGINATION ---------- */

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

    /* ---------- OPTIMISTIC LIKE ---------- */

    fun toggleLikeOptimistic(postId: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return

        val updated = current.map {
            if (it is FeedItem.Post && it.id == postId && !it.likedByMe) {
                it.copy(
                    likes = it.likes + 1,
                    likedByMe = true
                )
            } else it
        }

        _uiState.value = UiState.Success(updated)
    }

    /* ---------- DELETE ---------- */

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
            val current = (_uiState.value as? UiState.Success)?.data ?: return@launch
            _uiState.value =
                UiState.Success(current.filterNot { it is FeedItem.Post && it.id == postId })
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(announcementId)
            val current = (_uiState.value as? UiState.Success)?.data ?: return@launch
            _uiState.value =
                UiState.Success(current.filterNot { it is FeedItem.Announcement && it.id == announcementId })
        }
    }

    /* ---------- REFRESH ---------- */

    fun refreshFeed() {
        listener?.remove()
        listener = null
        lastVisible = null
        loadFeed()
    }

    /* ---------- MAPPER ---------- */

    private fun mapDoc(doc: DocumentSnapshot): FeedItem? {
        val type = doc.getString("type") ?: return null

        val time = when {
            doc.get("createdAt") is com.google.firebase.Timestamp ->
                (doc.get("createdAt") as com.google.firebase.Timestamp).toDate().time

            doc.get("createdAt") is Long ->
                doc.getLong("createdAt") ?: 0L

            else -> 0L
        }
        val currentUid = auth.currentUser?.uid

        return when (type) {
            "post" -> {
                val likedBy =
                    doc.get("likedBy") as? Map<*, *> ?: emptyMap<Any, Any>()

                val likedByMe =
                    currentUid != null && likedBy.containsKey(currentUid)

                FeedItem.Post(
                    id = doc.id,
                    author = doc.getString("authorName") ?: "User",
                    content = doc.getString("content") ?: "",
                    time = time,
                    likes = (doc.getLong("likes") ?: 0L).toInt(),
                    commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                    likedByMe = likedByMe,
                    imageUrl = doc.getString("imageUrl")
                )
            }

            "announcement" -> FeedItem.Announcement(
                id = doc.id,
                title = doc.getString("title") ?: "",
                message = doc.getString("content") ?: "",
                time = time
            )

            else -> null
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }

    fun addOptimistic(item: FeedItem) {
        val current = (_uiState.value as? UiState.Success)?.data ?: emptyList()
        _uiState.value = UiState.Success(listOf(item) + current)
    }
}
