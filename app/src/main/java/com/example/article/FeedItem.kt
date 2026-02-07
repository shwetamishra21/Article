package com.example.article

sealed class FeedItem {

    data class Post(
        val id: String,
        val author: String,
        val content: String,
        val time: Long,
        val likes: Int,
        val commentCount: Int,
        val likedByMe: Boolean,
        val imageUrl: String?    // ✅ ADD
    ) : FeedItem()


    data class Announcement(
        val id: String,
        val title: String,
        val message: String,
        val time: Long

    ) : FeedItem()
}
