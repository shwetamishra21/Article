package com.example.article

data class ChatThread(
    val id: String = "",
    val title: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,  // Changed from updatedAt for consistency
    val type: String = "member", // "service" or "member"
    val participants: List<String> = emptyList(),
    val unreadCount: Int = 0,  // NEW: Track unread messages
    val lastMessageSenderId: String = "",  // NEW: Who sent the last message
    val otherUserPhotoUrl: String = ""  // NEW: For displaying avatar
)

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false,  // NEW: Track if message is read
    val senderName: String = ""  // NEW: Display sender name
)