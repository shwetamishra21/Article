package com.example.article.Repository

import com.google.firebase.Timestamp

data class ServiceRequest(
    val id: String = "",
    val serviceType: String = "",
    val title: String = "",
    val description: String = "",
    val memberName: String = "",
    val memberNeighborhood: String = "",
    val createdBy: String = "",
    val assignedTo: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Timestamp = Timestamp.now(),
    val preferredDate: Timestamp? = null,
    val completedAt: Timestamp? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"

        const val COLLECTION_NAME = "service_requests"  // ✅ Added
    }

    // ✅ Added toMap() function
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "serviceType" to serviceType,
            "title" to title,
            "description" to description,
            "memberName" to memberName,
            "memberNeighborhood" to memberNeighborhood,
            "createdBy" to createdBy,
            "assignedTo" to assignedTo,
            "status" to status,
            "createdAt" to createdAt,
            "preferredDate" to preferredDate,
            "completedAt" to completedAt
        )
    }
}