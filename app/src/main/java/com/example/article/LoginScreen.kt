package com.example.article.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.UserSessionManager
import com.example.article.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSignUp by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BluePrimaryDark,
                        BluePrimary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = if (isSignUp) "Create Account" else "Welcome Back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BlueOnPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Article Community",
                fontSize = 16.sp,
                color = BlueOnPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BlueOnPrimary,
                    unfocusedBorderColor = BlueOnPrimary.copy(alpha = 0.5f),
                    focusedTextColor = BlueOnPrimary,
                    unfocusedTextColor = BlueOnPrimary,
                    focusedLabelColor = BlueOnPrimary,
                    unfocusedLabelColor = BlueOnPrimary.copy(alpha = 0.7f),
                    cursorColor = BlueOnPrimary
                ),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BlueOnPrimary,
                    unfocusedBorderColor = BlueOnPrimary.copy(alpha = 0.5f),
                    focusedTextColor = BlueOnPrimary,
                    unfocusedTextColor = BlueOnPrimary,
                    focusedLabelColor = BlueOnPrimary,
                    unfocusedLabelColor = BlueOnPrimary.copy(alpha = 0.7f),
                    cursorColor = BlueOnPrimary
                ),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ErrorLight.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        color = BlueOnPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login/Sign Up button
            Button(
                onClick = {
                    scope.launch {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill all fields"
                            return@launch
                        }

                        isLoading = true
                        errorMessage = null

                        try {
                            if (isSignUp) {
                                handleSignUp(
                                    auth = auth,
                                    firestore = firestore,
                                    email = email.trim(),
                                    password = password,
                                    onSuccess = onLoginSuccess,
                                    onError = { errorMessage = it }
                                )
                            } else {
                                handleLogin(
                                    auth = auth,
                                    firestore = firestore,
                                    email = email.trim(),
                                    password = password,
                                    onSuccess = onLoginSuccess,
                                    onError = { errorMessage = it }
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "An error occurred"
                            Log.e("LoginScreen", "Login error", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueOnPrimary,
                    contentColor = BluePrimaryDark
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BluePrimaryDark
                    )
                } else {
                    Text(
                        text = if (isSignUp) "Sign Up" else "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle between login and sign up
            TextButton(
                onClick = {
                    isSignUp = !isSignUp
                    errorMessage = null
                },
                enabled = !isLoading
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                    color = BlueOnPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ========================================
// BACKEND FUNCTIONS
// ========================================

private suspend fun handleLogin(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("LoginScreen", "Starting login for: $email")

        // 1. Firebase Auth login
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Login failed - no user returned")

        Log.d("LoginScreen", "Auth success, UID: ${user.uid}")

        // 2. Check if profile exists
        val profileDoc = firestore.collection("users").document(user.uid).get().await()

        Log.d("LoginScreen", "Profile exists: ${profileDoc.exists()}")

        // 3. Create profile if doesn't exist
        if (!profileDoc.exists()) {
            Log.d("LoginScreen", "Creating missing profile...")
            createUserProfile(user.uid, email, firestore)
        }

        // 4. Load profile into UserSessionManager
        Log.d("LoginScreen", "Loading profile into session...")
        val result = UserSessionManager.loadUserProfile(user.uid, firestore)

        if (result.isFailure) {
            Log.e("LoginScreen", "Failed to load profile", result.exceptionOrNull())
            throw Exception("Failed to load profile: ${result.exceptionOrNull()?.message}")
        }

        Log.d("LoginScreen", "Profile loaded successfully, calling onSuccess")

        // 5. Success - trigger navigation
        onSuccess()

    } catch (e: Exception) {
        Log.e("LoginScreen", "Login failed", e)
        onError(e.message ?: "Login failed")
    }
}

private suspend fun handleSignUp(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("LoginScreen", "Starting signup for: $email")

        // Validate password
        if (password.length < 6) {
            onError("Password must be at least 6 characters")
            return
        }

        // 1. Create Firebase Auth account
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Sign up failed - no user returned")

        Log.d("LoginScreen", "Auth account created, UID: ${user.uid}")

        // 2. Create Firestore profile
        Log.d("LoginScreen", "Creating Firestore profile...")
        createUserProfile(user.uid, email, firestore)

        // 3. Load profile into UserSessionManager
        Log.d("LoginScreen", "Loading profile into session...")
        val result = UserSessionManager.loadUserProfile(user.uid, firestore)

        if (result.isFailure) {
            Log.e("LoginScreen", "Failed to load profile", result.exceptionOrNull())
            throw Exception("Failed to load profile: ${result.exceptionOrNull()?.message}")
        }

        Log.d("LoginScreen", "Profile loaded successfully, calling onSuccess")

        // 4. Success - trigger navigation
        onSuccess()

    } catch (e: Exception) {
        Log.e("LoginScreen", "Signup failed", e)
        // Cleanup: If account created but profile failed, delete account
        try {
            auth.currentUser?.delete()?.await()
        } catch (deleteError: Exception) {
            Log.e("LoginScreen", "Failed to cleanup account", deleteError)
        }
        onError(e.message ?: "Sign up failed")
    }
}

private suspend fun createUserProfile(uid: String, email: String, firestore: FirebaseFirestore) {
    try {
        Log.d("LoginScreen", "Creating profile for UID: $uid")

        val defaultName = email.substringBefore("@").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        val userProfile = hashMapOf(
            "uid" to uid,
            "email" to email,
            "name" to defaultName,
            "role" to "member",
            "neighbourhood" to "",
            "bio" to "",
            "photoUrl" to "",
            "photoPublicId" to "",
            "createdAt" to Timestamp.now()
        )

        firestore.collection("users")
            .document(uid)
            .set(userProfile)
            .await()

        Log.d("LoginScreen", "Profile created successfully")

    } catch (e: Exception) {
        Log.e("LoginScreen", "Failed to create profile", e)
        throw Exception("Failed to create profile: ${e.message}")
    }
}