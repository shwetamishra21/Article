package com.example.article.Repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object RequestRepository {

    private val db = FirebaseFirestore.getInstance()
    private val requests = db.collection("requests")

    /* ================= CREATE ================= */

    suspend fun createRequest(
        title: String,
        description: String,
        serviceType: String,
        createdBy: String
    ) {
        val now = System.currentTimeMillis()

        val data = mapOf(
            "title" to title,
            "description" to description,
            "serviceType" to serviceType,
            "status" to ServiceRequest.STATUS_PENDING,
            "createdBy" to createdBy,
            "assignedTo" to null,
            "createdAt" to now,
            "updatedAt" to now
        )

        requests.add(data).await()
    }

    /* ================= READ ================= */

    // Member OR admin (as a normal user) → own requests
    suspend fun getUserRequests(uid: String): List<ServiceRequest> {
        val snapshot = requests
            .whereEqualTo("createdBy", uid)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toServiceRequest() }
    }

    // Provider → requests assigned to them
    suspend fun getProviderRequests(providerId: String): List<ServiceRequest> {
        val snapshot = requests
            .whereEqualTo("assignedTo", providerId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toServiceRequest() }
    }

    /* ================= UPDATE ================= */

    // Provider accepts a pending request
    suspend fun acceptRequest(
        requestId: String,
        providerId: String
    ) {
        requests.document(requestId)
            .update(
                mapOf(
                    "status" to ServiceRequest.STATUS_ACCEPTED,
                    "assignedTo" to providerId,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    // Provider completes an accepted request
    suspend fun completeRequest(requestId: String) {
        requests.document(requestId)
            .update(
                mapOf(
                    "status" to ServiceRequest.STATUS_COMPLETED,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    // Request owner cancels (member OR admin personal request)
    suspend fun cancelRequest(requestId: String) {
        requests.document(requestId)
            .update(
                mapOf(
                    "status" to ServiceRequest.STATUS_CANCELLED,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }
}
