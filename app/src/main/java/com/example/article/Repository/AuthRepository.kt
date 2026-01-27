package com.example.article.Repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw Exception("User creation failed")
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUserId(): String? = auth.currentUser?.uid
}
