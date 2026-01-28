package com.example.article.feed

sealed class FeedItem {
    data class Announcement(
        val id: String,
        val title: String,
        val message: String,
        val time: Long
    ) : FeedItem()

    data class Post(
        val id: String,
        val author: String,
        val content: String,
        val time: Long,
        val likes: Int
    ) : FeedItem()
}
