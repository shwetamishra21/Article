package com.example.article.Repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /**
     * All requests created by a specific member, newest first.
     *
     * WHY no orderBy: combining .whereEqualTo("memberId") + .orderBy("createdAt")
     * requires a composite Firestore index. Without that index deployed, Firestore
     * silently returns an error and the listener emits emptyList(). We sort in-memory
     * instead so it works without any index setup.
     */
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

    /**
     * All requests assigned to a specific provider (accepted / in_progress / completed).
     *
     * Same reasoning as getMemberRequests — single-field query, in-memory sort.
     */
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
     * Pending requests visible to providers on the home feed.
     * Optionally filtered by serviceType.
     *
     * WHY no .whereEqualTo("providerId", null):
     * Firestore null-equality filters are unreliable and often return nothing
     * (or require a special index). We query by status only and filter unassigned
     * documents in-memory. This is safe because a newly created request has
     * providerId = null in the data model and won't have that field set in Firestore,
     * so checking `it.providerId == null` in Kotlin correctly excludes claimed requests.
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
                    // Only show requests not yet claimed by any provider
                    .filter { it.providerId == null }
                    // Optionally narrow to a specific trade
                    .filter { serviceType == null || it.serviceType == serviceType }
                    .sortedByDescending { it.createdAt.seconds }
                trySend(results)
            }
        awaitClose { listener.remove() }
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

    /**
     * Create a brand-new service request from a member.
     * Returns the new document ID.
     */
    suspend fun createRequest(request: ServiceRequest): Result<String> = runCatching {
        val docRef = col.add(request.toMap()).await()
        Log.d(TAG, "Created request ${docRef.id}")
        docRef.id
    }

    /**
     * Member cancels a pending request.
     * Only allowed when status == pending.
     */
    /**
     * Member cancels a request.
     * Allowed for: pending (no provider yet) and accepted (provider assigned but not started).
     * When cancelling an accepted request, provider assignment is also cleared so the
     * provider's active count stays accurate.
     */
    suspend fun cancelRequest(requestId: String, memberId: String): Result<Unit> = runCatching {
        val doc = col.document(requestId).get().await()
        val status = doc.getString("status")
        val owner = doc.getString("memberId")

        require(owner == memberId) { "Not authorized to cancel this request" }
        require(
            status == ServiceRequest.STATUS_PENDING ||
                    status == ServiceRequest.STATUS_ACCEPTED
        ) {
            "Cannot cancel a request that is already $status"
        }

        val updates = mutableMapOf<String, Any?>(
            "status" to ServiceRequest.STATUS_CANCELLED,
            "updatedAt" to Timestamp.now()
        )

        // If a provider had already accepted, clear their assignment so their
        // active count reflects reality and the job doesn't linger in their feed
        if (status == ServiceRequest.STATUS_ACCEPTED) {
            updates["providerId"] = FieldValue.delete()
            updates["providerName"] = FieldValue.delete()
            updates["acceptedAt"] = FieldValue.delete()
        }

        col.document(requestId).update(updates).await()
        Log.d(TAG, "Request $requestId cancelled by member $memberId (was $status)")
    }

    /**
     * Member rates a completed request.
     */
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

        // Also update the provider's aggregate rating in their user document
        val providerId = doc.getString("providerId")
        if (providerId != null) {
            updateProviderRating(providerId, rating)
        }

        Log.d(TAG, "Request $requestId rated $rating by member $memberId")
    }

    // ─────────────────────────────────────────────
    // PROVIDER ACTIONS
    // ─────────────────────────────────────────────

    /**
     * Provider accepts a pending request.
     * Atomically sets providerId, providerName, status, acceptedAt.
     * Uses a transaction to prevent two providers claiming the same request simultaneously.
     */
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

    /**
     * Provider starts work — transitions accepted → in_progress.
     */
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

    /**
     * Provider marks as completed.
     */
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

    /**
     * Provider declines / backs out of a request.
     * byProvider=true  → resets to pending so another provider can claim it.
     * byProvider=false → admin/system cancel, marks cancelled.
     */
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

    /**
     * Admin hard-cancel: set status = cancelled regardless of current state.
     */
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

    /** Link a chat thread to a service request. */
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

    // Extension to map a Firestore QuerySnapshot into a typed list
    private fun com.google.firebase.firestore.QuerySnapshot.toRequests(): List<ServiceRequest> =
        documents.mapNotNull { doc ->
            try {
                doc.toObject(ServiceRequest::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Parse error for ${doc.id}", e)
                null
            }
        }
}