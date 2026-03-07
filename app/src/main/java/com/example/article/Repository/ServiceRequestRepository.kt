package com.example.article.Repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class ServiceRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col = firestore.collection(ServiceRequest.COLLECTION_NAME)

    companion object {
        private const val TAG = "ServiceRequestRepo"
    }

    // ─────────────────────────────────────────────
    // REAL-TIME STREAMS
    // ─────────────────────────────────────────────

    fun getMemberRequests(memberId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val listener = col
            .whereEqualTo("memberId", memberId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "getMemberRequests error", err)
                    trySend(emptyList()); return@addSnapshotListener
                }
                val sorted = (snap?.toRequests() ?: emptyList())
                    .sortedByDescending { it.createdAt.seconds }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    fun getProviderRequests(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val listener = col
            .whereEqualTo("providerId", providerId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "getProviderRequests error", err)
                    trySend(emptyList()); return@addSnapshotListener
                }
                val sorted = (snap?.toRequests() ?: emptyList())
                    .sortedByDescending { it.createdAt.seconds }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Pending requests visible to ALL providers (no neighbourhood filter).
     * Kept for backward compatibility / fallback.
     */
    fun getPendingRequests(serviceType: String? = null): Flow<List<ServiceRequest>> = callbackFlow {
        val listener = col
            .whereEqualTo("status", ServiceRequest.STATUS_PENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "getPendingRequests error", err)
                    trySend(emptyList()); return@addSnapshotListener
                }
                val results = (snap?.toRequests() ?: emptyList())
                    .filter { it.providerId == null }
                    .filter { serviceType == null || it.serviceType == serviceType }
                    .sortedByDescending { it.createdAt.seconds }
                trySend(results)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Pending requests filtered to a specific neighbourhood.
     *
     * Providers should only see requests from members of their own neighbourhood.
     * Firestore does not support "whereIn" with more than 30 items, so we fetch
     * all pending (unclaimed) requests and filter in-memory by the memberIds set.
     *
     * If memberUids is empty (neighbourhood has no members yet) we return an empty flow
     * immediately — no Firestore query needed.
     */
    fun getPendingRequestsForNeighbourhood(
        memberUids: List<String>,
        serviceType: String? = null
    ): Flow<List<ServiceRequest>> {
        if (memberUids.isEmpty()) return flowOf(emptyList())

        return callbackFlow {
            val memberUidSet = memberUids.toHashSet()

            val listener = col
                .whereEqualTo("status", ServiceRequest.STATUS_PENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.e(TAG, "getPendingRequestsForNeighbourhood error", err)
                        trySend(emptyList()); return@addSnapshotListener
                    }
                    val results = (snap?.toRequests() ?: emptyList())
                        .filter { it.providerId == null }
                        .filter { it.memberId in memberUidSet }
                        .filter { serviceType == null || it.serviceType == serviceType }
                        .sortedByDescending { it.createdAt.seconds }
                    trySend(results)
                }
            awaitClose { listener.remove() }
        }
    }

    // ─────────────────────────────────────────────
    // ONE-SHOT READS
    // ─────────────────────────────────────────────

    suspend fun getRequest(requestId: String): Result<ServiceRequest> = runCatching {
        val doc = col.document(requestId).get().await()
        doc.toObject(ServiceRequest::class.java)?.copy(id = doc.id)
            ?: error("Request $requestId not found")
    }

    // ─────────────────────────────────────────────
    // MEMBER ACTIONS
    // ─────────────────────────────────────────────

    suspend fun createRequest(request: ServiceRequest): Result<String> = runCatching {
        val docRef = col.add(request.toMap()).await()
        Log.d(TAG, "Created request ${docRef.id}")
        docRef.id
    }

    suspend fun cancelRequest(requestId: String, memberId: String): Result<Unit> = runCatching {
        val doc = col.document(requestId).get().await()
        val status = doc.getString("status")
        val owner = doc.getString("memberId")

        require(owner == memberId) { "Not authorized to cancel this request" }
        require(
            status == ServiceRequest.STATUS_PENDING ||
                    status == ServiceRequest.STATUS_ACCEPTED
        ) { "Cannot cancel a request that is already $status" }

        val updates = mutableMapOf<String, Any?>(
            "status" to ServiceRequest.STATUS_CANCELLED,
            "updatedAt" to Timestamp.now()
        )

        if (status == ServiceRequest.STATUS_ACCEPTED) {
            updates["providerId"] = FieldValue.delete()
            updates["providerName"] = FieldValue.delete()
            updates["acceptedAt"] = FieldValue.delete()
        }

        col.document(requestId).update(updates).await()
        Log.d(TAG, "Request $requestId cancelled by member $memberId (was $status)")
    }

    suspend fun rateRequest(
        requestId: String,
        memberId: String,
        rating: Float,
        review: String
    ): Result<Unit> = runCatching {
        val doc = col.document(requestId).get().await()
        require(doc.getString("memberId") == memberId) { "Not authorized" }
        require(doc.getString("status") == ServiceRequest.STATUS_COMPLETED) {
            "Can only rate completed requests"
        }

        col.document(requestId).update(
            mapOf(
                "rating" to rating,
                "review" to review,
                "updatedAt" to Timestamp.now()
            )
        ).await()

        val providerId = doc.getString("providerId")
        if (providerId != null) updateProviderRating(providerId, rating)

        Log.d(TAG, "Request $requestId rated $rating by member $memberId")
    }

    // ─────────────────────────────────────────────
    // PROVIDER ACTIONS
    // ─────────────────────────────────────────────

    suspend fun acceptRequest(
        requestId: String,
        providerId: String,
        providerName: String
    ): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val ref = col.document(requestId)
            val snap = tx.get(ref)
            val currentStatus = snap.getString("status")

            require(currentStatus == ServiceRequest.STATUS_PENDING) {
                "Request is no longer pending (status: $currentStatus)"
            }
            require(snap.getString("providerId") == null) {
                "Request already claimed by another provider"
            }

            tx.update(
                ref, mapOf(
                    "providerId" to providerId,
                    "providerName" to providerName,
                    "status" to ServiceRequest.STATUS_ACCEPTED,
                    "acceptedAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
            )
        }.await()
        Log.d(TAG, "Request $requestId accepted by provider $providerId")
    }

    suspend fun startWork(requestId: String): Result<Unit> = runCatching {
        col.document(requestId).update(
            mapOf(
                "status" to ServiceRequest.STATUS_IN_PROGRESS,
                "startedAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
        Log.d(TAG, "Request $requestId started")
    }

    suspend fun completeRequest(requestId: String): Result<Unit> = runCatching {
        col.document(requestId).update(
            mapOf(
                "status" to ServiceRequest.STATUS_COMPLETED,
                "completedAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
        Log.d(TAG, "Request $requestId completed")
    }

    suspend fun declineRequest(
        requestId: String,
        byProvider: Boolean = true
    ): Result<Unit> = runCatching {
        val updates: Map<String, Any?> = if (byProvider) {
            mapOf(
                "providerId" to FieldValue.delete(),
                "providerName" to FieldValue.delete(),
                "acceptedAt" to FieldValue.delete(),
                "startedAt" to FieldValue.delete(),
                "status" to ServiceRequest.STATUS_PENDING,
                "updatedAt" to Timestamp.now()
            )
        } else {
            mapOf(
                "status" to ServiceRequest.STATUS_CANCELLED,
                "updatedAt" to Timestamp.now()
            )
        }
        col.document(requestId).update(updates).await()
        Log.d(TAG, "Request $requestId declined (byProvider=$byProvider)")
    }

    // ─────────────────────────────────────────────
    // ADMIN ACTIONS
    // ─────────────────────────────────────────────

    suspend fun adminCancelRequest(requestId: String): Result<Unit> = runCatching {
        col.document(requestId).update(
            mapOf(
                "status" to ServiceRequest.STATUS_CANCELLED,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    // ─────────────────────────────────────────────
    // LINKING
    // ─────────────────────────────────────────────

    suspend fun linkChat(requestId: String, chatId: String): Result<Unit> = runCatching {
        col.document(requestId).update("chatId", chatId).await()
        Log.d(TAG, "Linked chat $chatId to request $requestId")
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private suspend fun updateProviderRating(providerId: String, newRating: Float) {
        try {
            val userRef = firestore.collection("users").document(providerId)
            firestore.runTransaction { tx ->
                val snap = tx.get(userRef)
                val currentAvg = (snap.getDouble("averageRating") ?: 0.0).toFloat()
                val count = (snap.getLong("ratingCount") ?: 0L).toInt()
                val newAvg = ((currentAvg * count) + newRating) / (count + 1)
                tx.update(
                    userRef, mapOf(
                        "averageRating" to newAvg,
                        "ratingCount" to count + 1
                    )
                )
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update provider rating", e)
        }
    }

    private fun com.google.firebase.firestore.QuerySnapshot.toRequests(): List<ServiceRequest> =
        documents.mapNotNull { doc ->
            try {
                doc.toObject(ServiceRequest::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Parse error for ${doc.id}", e); null
            }
        }
}