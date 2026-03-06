package com.example.article.Repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ==================== DATA MODELS ====================

data class Neighbourhood(
    val id: String = "",
    val name: String = "",
    val adminId: String = "",
    val description: String = "",
    val memberCount: Int = 0,
    val providerCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)

data class JoinRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "member",
    val neighbourhoodId: String = "",
    val status: String = "pending",   // pending | approved | rejected
    val createdAt: Timestamp = Timestamp.now()
)

// ==================== REPOSITORY ====================

object NeighbourhoodRepository {

    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "NeighbourhoodRepo"

    // ---------- NEIGHBOURHOOD CRUD ----------

    /**
     * Admin creates a neighbourhood.
     * Enforces one neighbourhood per admin by checking first.
     */
    suspend fun createNeighbourhood(
        adminId: String,
        name: String,
        description: String
    ): Result<String> {
        return try {
            // Enforce one neighbourhood per admin
            val existing = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminId)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("You already have a neighbourhood. Only one neighbourhood per admin is allowed."))
            }

            val data = hashMapOf(
                "name" to name,
                "description" to description,
                "adminId" to adminId,
                "memberCount" to 0,
                "providerCount" to 0,
                "createdAt" to Timestamp.now()
            )

            val ref = db.collection("neighbourhoods").add(data).await()
            Log.d(TAG, "Neighbourhood created: ${ref.id}")
            Result.success(ref.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create neighbourhood", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch all neighbourhoods (for Search screen).
     */
    suspend fun getAllNeighbourhoods(): Result<List<Neighbourhood>> {
        return try {
            val snapshot = db.collection("neighbourhoods").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    Neighbourhood(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        adminId = doc.getString("adminId") ?: "",
                        description = doc.getString("description") ?: "",
                        memberCount = doc.getLong("memberCount")?.toInt() ?: 0,
                        providerCount = doc.getLong("providerCount")?.toInt() ?: 0,
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing neighbourhood ${doc.id}", e)
                    null
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch neighbourhoods", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch the neighbourhood created by a specific admin.
     */
    suspend fun getNeighbourhoodByAdmin(adminId: String): Result<Neighbourhood?> {
        return try {
            val snapshot = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminId)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
            if (doc == null) {
                Result.success(null)
            } else {
                Result.success(
                    Neighbourhood(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        adminId = doc.getString("adminId") ?: "",
                        description = doc.getString("description") ?: "",
                        memberCount = doc.getLong("memberCount")?.toInt() ?: 0,
                        providerCount = doc.getLong("providerCount")?.toInt() ?: 0,
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch admin neighbourhood", e)
            Result.failure(e)
        }
    }

    // ---------- JOIN REQUESTS ----------

    /**
     * User sends a join request to a neighbourhood.
     * Guards:
     *  - User can only belong to one neighbourhood
     *  - No duplicate pending requests
     */
    suspend fun sendJoinRequest(
        userId: String,
        userName: String,
        userEmail: String,
        userRole: String,
        neighbourhoodId: String
    ): Result<Unit> {
        return try {
            // Guard 1: already a member somewhere
            val userDoc = db.collection("users").document(userId).get().await()
            val existingNeighId = userDoc.getString("neighbourhoodId") ?: ""
            if (existingNeighId.isNotEmpty()) {
                return Result.failure(Exception("You are already a member of a neighbourhood."))
            }

            // Guard 2: already has a pending request for this neighbourhood
            val pending = db.collection("join_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("neighbourhoodId", neighbourhoodId)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            if (!pending.isEmpty) {
                return Result.failure(Exception("You already have a pending request for this neighbourhood."))
            }

            val data = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "userEmail" to userEmail,
                "userRole" to userRole,
                "neighbourhoodId" to neighbourhoodId,
                "status" to "pending",
                "createdAt" to Timestamp.now()
            )

            db.collection("join_requests").add(data).await()
            Log.d(TAG, "Join request sent by $userId to neighbourhood $neighbourhoodId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send join request", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch pending join requests for the admin's neighbourhood.
     */
    suspend fun getPendingRequests(neighbourhoodId: String): Result<List<JoinRequest>> {
        return try {
            val snapshot = db.collection("join_requests")
                .whereEqualTo("neighbourhoodId", neighbourhoodId)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    JoinRequest(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        userRole = doc.getString("userRole") ?: "member",
                        neighbourhoodId = doc.getString("neighbourhoodId") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing join request ${doc.id}", e)
                    null
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch pending requests", e)
            Result.failure(e)
        }
    }

    /**
     * Admin approves a join request.
     * Atomically:
     *  1. Updates request status → approved
     *  2. Sets user.neighbourhoodId
     *  3. Increments memberCount or providerCount
     */
    suspend fun approveRequest(request: JoinRequest): Result<Unit> {
        return try {
            val batch = db.batch()

            val requestRef = db.collection("join_requests").document(request.id)
            val userRef = db.collection("users").document(request.userId)
            val neighRef = db.collection("neighbourhoods").document(request.neighbourhoodId)

            batch.update(requestRef, "status", "approved")
            // Uses "neighbourhoodId" to match your existing Firestore field name
            batch.update(userRef, "neighbourhoodId", request.neighbourhoodId)

            if (request.userRole == "service_provider") {
                batch.update(neighRef, "providerCount", FieldValue.increment(1))
            } else {
                batch.update(neighRef, "memberCount", FieldValue.increment(1))
            }

            batch.commit().await()
            Log.d(TAG, "Request ${request.id} approved")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to approve request", e)
            Result.failure(e)
        }
    }

    /**
     * Admin rejects a join request.
     */
    suspend fun rejectRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("join_requests")
                .document(requestId)
                .update("status", "rejected")
                .await()
            Log.d(TAG, "Request $requestId rejected")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reject request", e)
            Result.failure(e)
        }
    }

    /**
     * Admin removes a member from their neighbourhood.
     * Atomically:
     *  1. Clears user.neighbourhoodId
     *  2. Decrements memberCount or providerCount
     */
    suspend fun removeMemberFromNeighbourhood(
        userId: String,
        userRole: String,
        neighbourhoodId: String
    ): Result<Unit> {
        return try {
            val batch = db.batch()

            val userRef = db.collection("users").document(userId)
            val neighRef = db.collection("neighbourhoods").document(neighbourhoodId)

            batch.update(userRef, "neighbourhoodId", "")

            if (userRole == "service_provider") {
                batch.update(neighRef, "providerCount", FieldValue.increment(-1))
            } else {
                batch.update(neighRef, "memberCount", FieldValue.increment(-1))
            }

            batch.commit().await()
            Log.d(TAG, "User $userId removed from neighbourhood $neighbourhoodId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove member from neighbourhood", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a user already has a pending request for a given neighbourhood.
     * Used by SearchScreen to show correct button state.
     */
    suspend fun getUserRequestStatus(
        userId: String,
        neighbourhoodId: String
    ): String {
        return try {
            val snapshot = db.collection("join_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("neighbourhoodId", neighbourhoodId)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
            doc?.getString("status") ?: "none"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check request status", e)
            "none"
        }
    }
}