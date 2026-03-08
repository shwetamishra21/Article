package com.example.article.Repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val referenceId: String = ""
) {
    companion object {
        const val TYPE_ANNOUNCEMENT    = "announcement"
        const val TYPE_MESSAGE         = "message"
        const val TYPE_SERVICE_REQUEST = "service_request"
    }
}

object NotificationRepository {

    private const val TAG        = "NotificationRepository"
    private const val COLLECTION = "notifications"
    private val firestore        = FirebaseFirestore.getInstance()

    fun observeNotifications(recipientId: String): Flow<List<AppNotification>> =
        callbackFlow {
            val listener = firestore.collection(COLLECTION)
                .whereEqualTo("recipientId", recipientId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { Log.e(TAG, "observe error", error); return@addSnapshotListener }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            AppNotification(
                                id          = doc.id,
                                recipientId = doc.getString("recipientId") ?: "",
                                type        = doc.getString("type")        ?: "",
                                title       = doc.getString("title")       ?: "",
                                body        = doc.getString("body")        ?: "",
                                read        = doc.getBoolean("read")       ?: false,
                                createdAt   = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                                referenceId = doc.getString("referenceId") ?: ""
                            )
                        } catch (e: Exception) { Log.w(TAG, "parse error ${doc.id}", e); null }
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        firestore.collection(COLLECTION).document(notificationId).update("read", true).await()
    }

    suspend fun markAllAsRead(recipientId: String): Result<Unit> = runCatching {
        val unread = firestore.collection(COLLECTION)
            .whereEqualTo("recipientId", recipientId)
            .whereEqualTo("read", false)
            .get().await()
        val batch = firestore.batch()
        unread.documents.forEach { batch.update(it.reference, "read", true) }
        batch.commit().await()
    }

    suspend fun createNotification(notification: AppNotification): Result<String> = runCatching {
        firestore.collection(COLLECTION).add(
            mapOf(
                "recipientId" to notification.recipientId,
                "type"        to notification.type,
                "title"       to notification.title,
                "body"        to notification.body,
                "read"        to false,
                "createdAt"   to Timestamp.now(),
                "referenceId" to notification.referenceId
            )
        ).await().id
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
        firestore.collection(COLLECTION).document(notificationId).delete().await()
    }
}