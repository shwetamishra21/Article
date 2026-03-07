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
    val status: String = "pending",
    val createdAt: Timestamp = Timestamp.now()
)

// ==================== REPOSITORY ====================

object NeighbourhoodRepository {

    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "NeighbourhoodRepo"

    // ---------- NEIGHBOURHOOD CRUD ----------

    suspend fun createNeighbourhood(
        adminId: String,
        name: String,
        description: String
    ): Result<String> {
        return try {
            val existing = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminId)
                .get().await()

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
                    Log.e(TAG, "Error parsing neighbourhood ${doc.id}", e); null
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch neighbourhoods", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch a single neighbourhood by its document ID.
     */
    suspend fun getNeighbourhoodById(neighbourhoodId: String): Result<Neighbourhood?> {
        return try {
            val doc = db.collection("neighbourhoods").document(neighbourhoodId).get().await()
            if (!doc.exists()) return Result.success(null)
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch neighbourhood by id $neighbourhoodId", e)
            Result.failure(e)
        }
    }

    suspend fun getNeighbourhoodByAdmin(adminId: String): Result<Neighbourhood?> {
        return try {
            val snapshot = db.collection("neighbourhoods")
                .whereEqualTo("adminId", adminId).get().await()

            val doc = snapshot.documents.firstOrNull() ?: return Result.success(null)
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch admin neighbourhood", e)
            Result.failure(e)
        }
    }

    /**
     * Returns all Neighbourhood objects a provider has been approved into.
     * Derived from join_requests (userId == providerId AND status == "approved").
     * No schema changes needed — providers never get neighbourhoodId written to their user doc.
     */
    suspend fun getApprovedNeighbourhoodsForProvider(providerId: String): Result<List<Neighbourhood>> {
        return try {
            val snapshot = db.collection("join_requests")
                .whereEqualTo("userId", providerId)
                .whereEqualTo("status", "approved")
                .get().await()

            val neighbourhoodIds = snapshot.documents
                .mapNotNull { it.getString("neighbourhoodId") }
                .distinct()

            if (neighbourhoodIds.isEmpty()) return Result.success(emptyList())

            val neighbourhoods = neighbourhoodIds.mapNotNull { neighId ->
                getNeighbourhoodById(neighId).getOrNull()
            }

            Log.d(TAG, "Provider $providerId is in ${neighbourhoods.size} neighbourhood(s)")
            Result.success(neighbourhoods)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch approved neighbourhoods for provider $providerId", e)
            Result.failure(e)
        }
    }

    /**
     * Returns all member UIDs that belong to a given neighbourhood.
     * Used by providers to filter service requests to their neighbourhood(s).
     */
    suspend fun getMemberUidsOfNeighbourhood(neighbourhoodId: String): Result<List<String>> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("neighbourhoodId", neighbourhoodId)
                .get().await()

            val uids = snapshot.documents.mapNotNull { doc ->
                doc.getString("uid") ?: doc.id.takeIf { it.isNotBlank() }
            }
            Log.d(TAG, "Found ${uids.size} members in neighbourhood $neighbourhoodId")
            Result.success(uids)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch member UIDs for neighbourhood $neighbourhoodId", e)
            Result.failure(e)
        }
    }

    // ---------- JOIN REQUESTS ----------

    /**
     * Send a join request.
     *
     * Rules:
     *  - Members:  blocked if already in any neighbourhood (single neighbourhood limit enforced
     *              via user.neighbourhoodId field)
     *  - Providers: no single-neighbourhood limit; blocked only if they already have a
     *              pending OR approved request for THIS specific neighbourhood
     */
    suspend fun sendJoinRequest(
        userId: String,
        userName: String,
        userEmail: String,
        userRole: String,
        neighbourhoodId: String
    ): Result<Unit> {
        return try {
            // Members only: enforce single-neighbourhood limit
            if (userRole != "service_provider") {
                val userDoc = db.collection("users").document(userId).get().await()
                val existingNeighId = userDoc.getString("neighbourhoodId") ?: ""
                if (existingNeighId.isNotEmpty()) {
                    return Result.failure(Exception("You are already a member of a neighbourhood."))
                }
            }

            // Everyone: block duplicate pending requests for the same neighbourhood
            val pending = db.collection("join_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("neighbourhoodId", neighbourhoodId)
                .whereEqualTo("status", "pending")
                .get().await()

            if (!pending.isEmpty) {
                return Result.failure(Exception("You already have a pending request for this neighbourhood."))
            }

            // Providers: also block if already approved for this specific neighbourhood
            if (userRole == "service_provider") {
                val approved = db.collection("join_requests")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("neighbourhoodId", neighbourhoodId)
                    .whereEqualTo("status", "approved")
                    .get().await()

                if (!approved.isEmpty) {
                    return Result.failure(Exception("You are already a member of this neighbourhood."))
                }
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
            Log.d(TAG, "Join request sent by $userId ($userRole) to neighbourhood $neighbourhoodId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send join request", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingRequests(neighbourhoodId: String): Result<List<JoinRequest>> {
        return try {
            val snapshot = db.collection("join_requests")
                .whereEqualTo("neighbourhoodId", neighbourhoodId)
                .whereEqualTo("status", "pending")
                .get().await()

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
                    Log.e(TAG, "Error parsing join request ${doc.id}", e); null
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch pending requests", e)
            Result.failure(e)
        }
    }

    /**
     * Approve a join request.
     *
     * - Members:   writes neighbourhoodId to user doc + increments memberCount (unchanged)
     * - Providers: does NOT touch user doc (they derive membership from join_requests);
     *              only increments providerCount
     */
    suspend fun approveRequest(request: JoinRequest): Result<Unit> {
        return try {
            val batch = db.batch()
            val requestRef = db.collection("join_requests").document(request.id)
            val neighRef = db.collection("neighbourhoods").document(request.neighbourhoodId)

            batch.update(requestRef, "status", "approved")

            if (request.userRole == "service_provider") {
                // Providers: don't overwrite user.neighbourhoodId — they can be in multiple
                batch.update(neighRef, "providerCount", FieldValue.increment(1))
            } else {
                // Members: set their single neighbourhoodId
                val userRef = db.collection("users").document(request.userId)
                batch.update(userRef, "neighbourhoodId", request.neighbourhoodId)
                batch.update(neighRef, "memberCount", FieldValue.increment(1))
            }

            batch.commit().await()
            Log.d(TAG, "Request ${request.id} approved (role: ${request.userRole})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to approve request", e)
            Result.failure(e)
        }
    }

    suspend fun rejectRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("join_requests").document(requestId)
                .update("status", "rejected").await()
            Log.d(TAG, "Request $requestId rejected")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reject request", e)
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromNeighbourhood(
        userId: String,
        userRole: String,
        neighbourhoodId: String
    ): Result<Unit> {
        return try {
            val batch = db.batch()
            val neighRef = db.collection("neighbourhoods").document(neighbourhoodId)

            if (userRole == "service_provider") {
                // Providers: mark their approved join_request as removed + decrement count
                batch.update(neighRef, "providerCount", FieldValue.increment(-1))
                val approvedSnap = db.collection("join_requests")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("neighbourhoodId", neighbourhoodId)
                    .whereEqualTo("status", "approved")
                    .get().await()
                approvedSnap.documents.forEach { doc ->
                    batch.update(doc.reference, "status", "removed")
                }
            } else {
                // Members: clear user.neighbourhoodId + decrement count
                val userRef = db.collection("users").document(userId)
                batch.update(userRef, "neighbourhoodId", "")
                batch.update(neighRef, "memberCount", FieldValue.increment(-1))
            }

            batch.commit().await()
            Log.d(TAG, "User $userId ($userRole) removed from neighbourhood $neighbourhoodId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove member from neighbourhood", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRequestStatus(userId: String, neighbourhoodId: String): String {
        return try {
            val snapshot = db.collection("join_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("neighbourhoodId", neighbourhoodId)
                .get().await()

            // Return the most relevant status: approved > pending > rejected > none
            val statuses = snapshot.documents.mapNotNull { it.getString("status") }
            when {
                "approved" in statuses -> "approved"
                "pending"  in statuses -> "pending"
                "rejected" in statuses -> "rejected"
                else                   -> "none"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check request status", e)
            "none"
        }
    }
}