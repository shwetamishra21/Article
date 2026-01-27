package com.example.article.Repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun createUserIfMissing(
        uid: String,
        email: String,
        role: String = "member"
    ) {
        val ref = db.collection("users").document(uid)
        val snapshot = ref.get().await()

        if (!snapshot.exists()) {
            val user = mapOf(
                "email" to email,
                "role" to role,
                "createdAt" to System.currentTimeMillis()
            )
            ref.set(user).await()
        }
    }

    suspend fun getUserRole(uid: String): String {
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.getString("role") ?: "member"
    }
}
