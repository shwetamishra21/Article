package com.example.article.Repository

import com.google.firebase.Timestamp

/**
 * Unified ServiceRequest data class.
 * Used consistently across Member, Provider, and Admin roles.
 */
data class ServiceRequest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val serviceType: String = "",

    // Member fields
    val memberId: String = "",
    val memberName: String = "",
    val memberNeighborhood: String = "",

    // Provider fields
    val providerId: String? = null,
    val providerName: String? = null,

    // Status and timestamps
    val status: String = STATUS_PENDING,
    val preferredDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,
    val acceptedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,

    // Chat reference
    val chatId: String? = null,

    // Rating (written by member after completion)
    val rating: Float? = null,
    val review: String? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_DECLINED = "declined"

        const val COLLECTION_NAME = "service_requests"
    }

    /** Convert to Firestore map for creating/updating documents. */
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
        "acceptedAt" to acceptedAt,
        "startedAt" to startedAt,
        "chatId" to chatId,
        "rating" to rating,
        "review" to review
    )

    /** True if this request is in an active (non-terminal) state. */
    val isActive: Boolean
        get() = status in listOf(STATUS_PENDING, STATUS_ACCEPTED, STATUS_IN_PROGRESS)

    /** True if the request can still be cancelled by the member. */
    val isCancellable: Boolean
        get() = status == STATUS_PENDING

    /** True if the request has a provider assigned. */
    val hasProvider: Boolean
        get() = providerId != null
}