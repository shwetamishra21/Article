package com.example.article.Repository

import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toServiceRequest(): ServiceRequest? {
    return ServiceRequest(
        id = id,
        title = getString("title") ?: return null,
        description = getString("description") ?: "",
        serviceType = getString("serviceType") ?: "",
        status = getString("status") ?: ServiceRequest.STATUS_PENDING,
        createdBy = getString("createdBy") ?: "",
        assignedTo = getString("assignedTo"),
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L
    )
}
