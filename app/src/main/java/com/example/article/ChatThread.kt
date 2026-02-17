package com.example.article.chat

import com.google.firebase.Timestamp

/**
 * Enhanced Chat Thread Model
 * Supports member-to-member and member-to-provider conversations
 */
data class ChatThread(
    val id: String = "",
    val participants: List<String> = emptyList(), // UIDs of all participants
    val participantNames: Map<String, String> = emptyMap(), // UID -> Name mapping
    val participantPhotos: Map<String, String> = emptyMap(), // UID -> Photo URL mapping
    val participantRoles: Map<String, String> = emptyMap(), // UID -> Role mapping (NEW)
    val type: String = "member", // "member" or "service"
    val title: String = "",
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Timestamp = Timestamp.now(),
    val unreadCounts: Map<String, Int> = emptyMap(), // UID -> unread count
    val typingUsers: List<String> = emptyList(), // UIDs of users currently typing
    val createdAt: Timestamp = Timestamp.now(),

    // Service-specific fields (optional)
    val serviceRequestId: String? = null,
    val serviceType: String? = null,
    val serviceStatus: String? = null
) {
    companion object {
        const val TYPE_MEMBER = "member"
        const val TYPE_SERVICE = "service"
    }

    /**
     * Get the other user's ID (for 1-on-1 chats)
     */
    fun getOtherUserId(currentUserId: String): String? {
        return participants.firstOrNull { it != currentUserId }
    }

    /**
     * Get the other user's name
     */
    fun getOtherUserName(currentUserId: String): String {
        val otherUserId = getOtherUserId(currentUserId) ?: return "Unknown"
        return participantNames[otherUserId] ?: "Unknown User"
    }

    /**
     * Get the other user's photo
     */
    fun getOtherUserPhoto(currentUserId: String): String {
        val otherUserId = getOtherUserId(currentUserId) ?: return ""
        return participantPhotos[otherUserId] ?: ""
    }

    /**
     * Get the other user's role
     */
    fun getOtherUserRole(currentUserId: String): String {
        val otherUserId = getOtherUserId(currentUserId) ?: return "member"
        return participantRoles[otherUserId] ?: "member"
    }

    /**
     * Get unread count for current user
     */
    fun getUnreadCount(currentUserId: String): Int {
        return unreadCounts[currentUserId] ?: 0
    }

    /**
     * Check if other user is typing
     */
    fun isOtherUserTyping(currentUserId: String): Boolean {
        return typingUsers.any { it != currentUserId }
    }
}

/**
 * Enhanced Message Model
 */
data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),

    // Message status
    val readBy: List<String> = emptyList(), // UIDs of users who read this message
    val deliveredTo: List<String> = emptyList(), // UIDs of users who received this message

    // Media support (future enhancement)
    val imageUrl: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,

    // System messages
    val isSystemMessage: Boolean = false,
    val systemMessageType: String? = null // "request_accepted", "request_completed", etc.
) {
    fun isReadBy(userId: String): Boolean = userId in readBy
    fun isDeliveredTo(userId: String): Boolean = userId in deliveredTo
}

/**
 * Typing Indicator Model (ephemeral - not stored in Firestore)
 */
data class TypingIndicator(
    val chatId: String,
    val userId: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)