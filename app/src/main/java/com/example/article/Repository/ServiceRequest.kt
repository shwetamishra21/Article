package com.example.article.Repository

import com.google.firebase.Timestamp

/**
 * Unified ServiceRequest data class
 * Compatible with both member and provider workflows
 */
data class ServiceRequest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val serviceType: String = "",

    // Member fields (using both old and new naming for compatibility)
    val memberId: String = "",          // New: preferred
    val createdBy: String = memberId,   // Old: alias for memberId
    val memberName: String = "",
    val memberNeighborhood: String = "",

    // Provider fields (using both old and new naming for compatibility)
    val providerId: String? = null,     // New: preferred
    val assignedTo: String? = providerId, // Old: alias for providerId
    val providerName: String? = null,

    // Status and dates
    val status: String = STATUS_PENDING,
    val preferredDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,

    // Additional fields
    val chatId: String? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"

        const val COLLECTION_NAME = "service_requests"
    }

    /**
     * Convert to Firestore map
     * Uses new field names (memberId, providerId) for consistency
     */
    fun toMap(): Map<String, Any?> = hashMapOf(
        "title" to title,
        "description" to description,
        "serviceType" to serviceType,
        "memberId" to memberId,
        "memberName" to memberName,
        "memberNeighborhood" to memberNeighborhood,
        "providerId" to providerId,
        "providerName" to providerName,
        "status" to status,
        "preferredDate" to preferredDate,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "completedAt" to completedAt,
        "chatId" to chatId
    )
}