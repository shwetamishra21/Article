package com.example.article.Repository

import com.google.firebase.Timestamp

data class ServiceRequest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val serviceType: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val memberNeighborhood: String = "",
    val providerId: String? = null,
    val providerName: String? = null,
    val status: String = STATUS_PENDING,
    val preferredDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,
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

    fun toMap(): Map<String, Any?> = hashMapOf(
        "id" to id,
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