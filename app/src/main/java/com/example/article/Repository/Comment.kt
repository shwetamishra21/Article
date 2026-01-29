package com.example.article

data class Comment(
    val id: String,
    val postId: String,
    val author: String,

    val authorId: String,
    val message: String,
    val createdAt: Long
)
