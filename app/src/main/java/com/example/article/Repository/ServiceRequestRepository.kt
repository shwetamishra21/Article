package com.example.article.Repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ServiceRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val requestsCollection = firestore.collection(ServiceRequest.COLLECTION_NAME)

    companion object {
        private const val TAG = "ServiceRequestRepo"
    }

    /**
     * Get all requests assigned to a specific provider in real-time
     */
    fun getProviderRequests(providerId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("providerId", providerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to provider requests", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ServiceRequest::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing request ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Loaded ${requests.size} requests for provider $providerId")
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all pending requests (not yet assigned to a provider)
     */
    fun getPendingRequests(): Flow<List<ServiceRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("status", ServiceRequest.STATUS_PENDING)
            .whereEqualTo("providerId", null)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to pending requests", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ServiceRequest::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing pending request ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Loaded ${requests.size} pending requests")
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Accept a service request (assign to provider)
     */
    suspend fun acceptRequest(
        requestId: String,
        providerId: String,
        providerName: String
    ): Result<Unit> = try {
        val updates = hashMapOf<String, Any>(
            "providerId" to providerId,
            "providerName" to providerName,
            "status" to ServiceRequest.STATUS_ACCEPTED,
            "updatedAt" to Timestamp.now()
        )

        requestsCollection.document(requestId)
            .update(updates)
            .await()

        Log.d(TAG, "Request $requestId accepted by provider $providerId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error accepting request $requestId", e)
        Result.failure(e)
    }

    /**
     * Mark request as in progress
     */
    suspend fun startWork(requestId: String): Result<Unit> = try {
        val updates = hashMapOf<String, Any>(
            "status" to ServiceRequest.STATUS_IN_PROGRESS,
            "updatedAt" to Timestamp.now()
        )

        requestsCollection.document(requestId)
            .update(updates)
            .await()

        Log.d(TAG, "Request $requestId marked as in progress")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error starting work on request $requestId", e)
        Result.failure(e)
    }

    /**
     * Complete a service request
     */
    suspend fun completeRequest(requestId: String): Result<Unit> = try {
        val updates = hashMapOf<String, Any>(
            "status" to ServiceRequest.STATUS_COMPLETED,
            "completedAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        requestsCollection.document(requestId)
            .update(updates)
            .await()

        Log.d(TAG, "Request $requestId marked as completed")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error completing request $requestId", e)
        Result.failure(e)
    }

    /**
     * Decline/Cancel a request
     */
    suspend fun declineRequest(requestId: String, byProvider: Boolean = false): Result<Unit> = try {
        val updates = if (byProvider) {
            // Provider declining after acceptance - reset to pending
            hashMapOf<String, Any>(
                "providerId" to com.google.firebase.firestore.FieldValue.delete(),
                "providerName" to com.google.firebase.firestore.FieldValue.delete(),
                "status" to ServiceRequest.STATUS_PENDING,
                "updatedAt" to Timestamp.now()
            )
        } else {
            // Cancelling completely
            hashMapOf<String, Any>(
                "status" to ServiceRequest.STATUS_CANCELLED,
                "updatedAt" to Timestamp.now()
            )
        }

        requestsCollection.document(requestId)
            .update(updates)
            .await()

        Log.d(TAG, "Request $requestId declined/cancelled")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error declining request $requestId", e)
        Result.failure(e)
    }

    /**
     * Get a single request by ID
     */
    suspend fun getRequest(requestId: String): Result<ServiceRequest> = try {
        val doc = requestsCollection.document(requestId).get().await()
        val request = doc.toObject(ServiceRequest::class.java)?.copy(id = doc.id)

        if (request != null) {
            Result.success(request)
        } else {
            Result.failure(Exception("Request not found"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting request $requestId", e)
        Result.failure(e)
    }

    /**
     * Create a chat for a request
     */
    suspend fun createRequestChat(requestId: String, chatId: String): Result<Unit> = try {
        requestsCollection.document(requestId)
            .update("chatId", chatId)
            .await()

        Log.d(TAG, "Chat $chatId linked to request $requestId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error linking chat to request", e)
        Result.failure(e)
    }

    /**
     * Get all requests created by a specific member in real-time
     */
    fun getMemberRequests(memberId: String): Flow<List<ServiceRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("memberId", memberId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to member requests", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ServiceRequest::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing request ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Loaded ${requests.size} requests for member $memberId")
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Create a new service request
     */
    suspend fun createRequest(request: ServiceRequest): Result<String> = try {
        val docRef = requestsCollection
            .add(request.toMap())
            .await()

        Log.d(TAG, "Request created with ID: ${docRef.id}")
        Result.success(docRef.id)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating request", e)
        Result.failure(e)
    }

    /**
     * Update request status
     */
    suspend fun updateRequestStatus(
        requestId: String,
        status: String
    ): Result<Unit> = try {
        val updates = hashMapOf<String, Any>(
            "status" to status,
            "updatedAt" to Timestamp.now()
        )

        requestsCollection.document(requestId)
            .update(updates)
            .await()

        Log.d(TAG, "Request $requestId status updated to $status")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating request status", e)
        Result.failure(e)
    }
}