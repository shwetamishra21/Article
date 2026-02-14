package com.example.article.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.UserSessionManager
import com.example.article.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("member") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }

    val roleOptions = listOf(
        "member" to "Member",
        "admin" to "Admin",
        "service_provider" to "Service Provider"
    )

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

            // Name field (only for sign up)
            if (isSignUp) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Full Name") },
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

                // Role Selection Dropdown (only for sign up)
                ExposedDropdownMenuBox(
                    expanded = showRoleMenu,
                    onExpandedChange = { showRoleMenu = !showRoleMenu && !isLoading }
                ) {
                    OutlinedTextField(
                        value = roleOptions.find { it.first == selectedRole }?.second ?: "Member",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRoleMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueOnPrimary,
                            unfocusedBorderColor = BlueOnPrimary.copy(alpha = 0.5f),
                            focusedTextColor = BlueOnPrimary,
                            unfocusedTextColor = BlueOnPrimary,
                            focusedLabelColor = BlueOnPrimary,
                            unfocusedLabelColor = BlueOnPrimary.copy(alpha = 0.7f)
                        ),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false },
                        modifier = Modifier.background(SurfaceLight)
                    ) {
                        roleOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = OnSurfaceLight) },
                                onClick = {
                                    selectedRole = value
                                    showRoleMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

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

            // Password field with visibility toggle
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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = BlueOnPrimary.copy(alpha = 0.7f)
                        )
                    }
                },
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
                    ),
                    shape = RoundedCornerShape(8.dp)
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
                        // Validation
                        val validationError = validateInput(email, password, name, isSignUp)
                        if (validationError != null) {
                            errorMessage = validationError
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
                                    name = name.trim(),
                                    role = selectedRole,
                                    onSuccess = {
                                        Log.d("LoginScreen", "SignUp success, navigating...")
                                        onLoginSuccess()
                                    },
                                    onError = { error ->
                                        Log.e("LoginScreen", "SignUp error: $error")
                                        errorMessage = error
                                    }
                                )
                            } else {
                                handleLogin(
                                    auth = auth,
                                    firestore = firestore,
                                    email = email.trim(),
                                    password = password,
                                    onSuccess = {
                                        Log.d("LoginScreen", "Login success, navigating...")
                                        onLoginSuccess()
                                    },
                                    onError = { error ->
                                        Log.e("LoginScreen", "Login error: $error")
                                        errorMessage = error
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("LoginScreen", "Unexpected error", e)
                            errorMessage = "An unexpected error occurred. Please try again."
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
                        color = BluePrimaryDark,
                        strokeWidth = 2.dp
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
                    // Reset role to member when switching
                    selectedRole = "member"
                    // Clear fields when switching
                    if (!isSignUp) {
                        name = ""
                    }
                },
                enabled = !isLoading
            ) {
                Text(
                    text = if (isSignUp)
                        "Already have an account? Login"
                    else
                        "Don't have an account? Sign Up",
                    color = BlueOnPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ========================================
// VALIDATION
// ========================================

private fun validateInput(email: String, password: String, name: String, isSignUp: Boolean): String? {
    return when {
        email.isBlank() -> "Email is required"
        password.isBlank() -> "Password is required"
        isSignUp && name.isBlank() -> "Name is required"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
        password.length < 6 -> "Password must be at least 6 characters"
        isSignUp && name.length < 2 -> "Name must be at least 2 characters"
        else -> null
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
        val user = authResult.user

        if (user == null) {
            Log.e("LoginScreen", "Auth returned null user")
            onError("Login failed. Please try again.")
            return
        }

        Log.d("LoginScreen", "Auth success, UID: ${user.uid}")

        // 2. Check if profile exists in Firestore
        val profileDoc = firestore.collection("users").document(user.uid).get().await()

        Log.d("LoginScreen", "Profile exists: ${profileDoc.exists()}")

        // 3. Create profile if it doesn't exist (edge case)
        if (!profileDoc.exists()) {
            Log.d("LoginScreen", "Creating missing profile...")
            createUserProfile(user.uid, email, "User", "member", firestore)
        }

        // 4. ALWAYS load profile AFTER creation check
        Log.d("LoginScreen", "Loading profile into session...")
        val result = UserSessionManager.loadUserProfile(user.uid, firestore)

        if (result.isFailure) {
            Log.e("LoginScreen", "Failed to load profile", result.exceptionOrNull())
            onError("Failed to load user data. Please try again.")
            return
        }

        Log.d("LoginScreen", "Profile loaded successfully")

        // 5. Trigger UI state update
        onSuccess()

    } catch (e: FirebaseAuthInvalidUserException) {
        Log.e("LoginScreen", "Invalid user", e)
        onError("No account found with this email")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Log.e("LoginScreen", "Invalid credentials", e)
        onError("Incorrect password")
    } catch (e: Exception) {
        Log.e("LoginScreen", "Login failed", e)
        onError(e.message ?: "Login failed. Please try again.")
    }
}

private suspend fun handleSignUp(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    password: String,
    name: String,
    role: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("LoginScreen", "Starting signup for: $email with role: $role")

        // 1. Create Firebase Auth account
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user

        if (user == null) {
            Log.e("LoginScreen", "Auth returned null user")
            onError("Sign up failed. Please try again.")
            return
        }

        Log.d("LoginScreen", "Auth account created, UID: ${user.uid}")

        // 2. Create Firestore profile with selected role
        Log.d("LoginScreen", "Creating Firestore profile with role: $role")
        createUserProfile(user.uid, email, name, role, firestore)

        // 3. ALWAYS load profile AFTER creation
        Log.d("LoginScreen", "Loading profile into session...")
        val result = UserSessionManager.loadUserProfile(user.uid, firestore)

        if (result.isFailure) {
            Log.e("LoginScreen", "Failed to load profile", result.exceptionOrNull())
            try {
                user.delete().await()
                Log.d("LoginScreen", "Cleaned up auth account after profile creation failure")
            } catch (deleteError: Exception) {
                Log.e("LoginScreen", "Failed to cleanup account", deleteError)
            }
            onError("Failed to create user profile. Please try again.")
            return
        }

        Log.d("LoginScreen", "Profile loaded successfully")

        // 4. Trigger UI state update
        onSuccess()

    } catch (e: FirebaseAuthWeakPasswordException) {
        Log.e("LoginScreen", "Weak password", e)
        onError("Password is too weak. Use at least 6 characters.")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Log.e("LoginScreen", "Invalid email", e)
        onError("Invalid email format")
    } catch (e: FirebaseAuthUserCollisionException) {
        Log.e("LoginScreen", "User exists", e)
        onError("An account with this email already exists")
    } catch (e: Exception) {
        Log.e("LoginScreen", "Signup failed", e)
        try {
            auth.currentUser?.delete()?.await()
        } catch (deleteError: Exception) {
            Log.e("LoginScreen", "Failed to cleanup account", deleteError)
        }
        onError(e.message ?: "Sign up failed. Please try again.")
    }
}

private suspend fun createUserProfile(
    uid: String,
    email: String,
    name: String,
    role: String,
    firestore: FirebaseFirestore
) {
    try {
        Log.d("LoginScreen", "Creating profile for UID: $uid with role: $role")

        val userProfile = hashMapOf(
            "uid" to uid,
            "email" to email,
            "name" to name,
            "role" to role,
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

        Log.d("LoginScreen", "Profile created successfully with role: $role")

    } catch (e: Exception) {
        Log.e("LoginScreen", "Failed to create profile", e)
        throw Exception("Failed to create user profile: ${e.message}")
    }
}