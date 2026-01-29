package com.example.article

sealed class FeedItem {

    data class Post(
        val id: String,
        val author: String,
        val content: String,
        val likes: Int,
        val commentCount: Int,
        val time: Long
    ) : FeedItem()

    data class Announcement(
        val id: String,
        val title: String,
        val message: String,
        val time: Long
    ) : FeedItem()
}
