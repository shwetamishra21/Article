package com.example.article.feed

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com. example. article. FeedItem

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _feed = MutableStateFlow<List<FeedItem>>(emptyList())
    val feed: StateFlow<List<FeedItem>> = _feed

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var lastVisible: DocumentSnapshot? = null
    private var listener: ListenerRegistration? = null

    private val PAGE_SIZE = 20

    /* ---------- REALTIME INITIAL LOAD ---------- */

    fun loadFeed() {
        if (listener != null) return

        _loading.value = true

        listener = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
            .addSnapshotListener { snapshot, error ->
                if (snapshot == null || error != null) {
                    _loading.value = false
                    return@addSnapshotListener
                }

                lastVisible = snapshot.documents.lastOrNull()

                _feed.value = snapshot.documents.mapNotNull { mapDoc(it) }
                _loading.value = false
            }
    }

    /* ---------- OPTIMISTIC INSERT ---------- */

    fun addOptimistic(item: FeedItem) {
        _feed.value = listOf(item) + _feed.value
    }

    /* ---------- PAGINATION ---------- */

    fun loadMore() {
        val last = lastVisible ?: return

        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .startAfter(last)
            .limit(PAGE_SIZE.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener

                lastVisible = snapshot.documents.last()

                val more = snapshot.documents.mapNotNull { mapDoc(it) }

                _feed.value = (_feed.value + more)
                    .distinctBy {
                        when (it) {
                            is FeedItem.Post -> it.id
                            is FeedItem.Announcement -> it.id
                        }
                    }
            }
    }

    /* ---------- MAPPER ---------- */

    private fun mapDoc(doc: DocumentSnapshot): FeedItem? {
        val type = doc.getString("type") ?: return null
        val time = doc.getLong("createdAt") ?: 0L

        return when (type) {
            "announcement" -> FeedItem.Announcement(
                id = doc.id,
                title = doc.getString("title") ?: "",
                message = doc.getString("content") ?: "",
                time = time
            )

            "post" -> FeedItem.Post(
                id = doc.id,
                author = doc.getString("authorName") ?: "User",
                content = doc.getString("content") ?: "",
                time = time,
                likes = (doc.getLong("likes") ?: 0L).toInt(),
                commentCount = (doc.getLong("commentCount") ?: 0L).toInt()
            )

            else -> null
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
