package com.example.article

data class ChatThread(
    val id: String,
    val title: String,
    val lastMessage: String,
    val updatedAt: Long,
    val type: String // "service" or "member"
)


data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L
)

