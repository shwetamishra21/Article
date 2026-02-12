package com.example.article

import com.google.firebase.Timestamp

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String = "",
    val content: String,
    val createdAt: Timestamp
)
