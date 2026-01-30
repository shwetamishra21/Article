package com.example.article.Repository

data class ServiceRequest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val serviceType: String = "",
    val status: String = STATUS_PENDING, // pending | accepted | completed | cancelled
    val createdBy: String = "",
    val assignedTo: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
    }
}
