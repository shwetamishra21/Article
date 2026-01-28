package com.example.article.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _feed = MutableStateFlow<List<FeedItem>>(emptyList())
    val feed: StateFlow<List<FeedItem>> = _feed

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var lastSnapshot = null as com.google.firebase.firestore.DocumentSnapshot?
    private val PAGE_SIZE = 10

    fun loadFeed(reset: Boolean = false) {
        if (_loading.value) return

        _loading.value = true

        var query = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())

        if (!reset && lastSnapshot != null) {
            query = query.startAfter(lastSnapshot!!)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    lastSnapshot = snapshot.documents.last()
                }

                val newItems = snapshot.documents.mapNotNull { doc ->
                    when (doc.getString("type")) {
                        "announcement" -> FeedItem.Announcement(
                            id = doc.id,
                            title = doc.getString("title") ?: "Announcement",
                            message = doc.getString("content") ?: "",
                            time = doc.getLong("createdAt") ?: 0L
                        )
                        "post" -> FeedItem.Post(
                            id = doc.id,
                            author = doc.getString("author") ?: "User",
                            content = doc.getString("content") ?: "",
                            time = doc.getLong("createdAt") ?: 0L,
                            likes = (doc.getLong("likes") ?: 0L).toInt()
                        )
                        else -> null
                    }
                }

                _feed.value =
                    if (reset) newItems
                    else _feed.value + newItems

                _loading.value = false
            }
            .addOnFailureListener {
                _loading.value = false
            }
    }

    fun addOptimistic(item: FeedItem) {
        _feed.value = listOf(item) + _feed.value
    }
}