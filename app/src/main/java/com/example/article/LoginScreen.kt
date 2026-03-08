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
import androidx.compose.ui.window.Dialog
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

    // Password reset state
    var showForgotPassword by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var resetIsError by remember { mutableStateOf(false) }
    var isResetLoading by remember { mutableStateOf(false) }

    // Email verification state
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isResendingVerification by remember { mutableStateOf(false) }
    var resendMessage by remember { mutableStateOf<String?>(null) }

    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }

    val roleOptions = listOf(
        "member" to "Member",
        "admin" to "Admin",
        "service_provider" to "Service Provider"
    )

    // ========================================
    // FORGOT PASSWORD DIALOG
    // ========================================
    if (showForgotPassword) {
        Dialog(onDismissRequest = {
            if (!isResetLoading) {
                showForgotPassword = false
                resetEmail = ""
                resetMessage = null
                resetIsError = false
            }
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Reset Password",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        fontSize = 14.sp,
                        color = OnSurfaceLight.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetMessage = null
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isResetLoading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Success / error message
                    if (resetMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (resetIsError)
                                    ErrorLight.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = resetMessage!!,
                                fontSize = 13.sp,
                                color = if (resetIsError) ErrorLight else OnSurfaceLight,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showForgotPassword = false
                                resetEmail = ""
                                resetMessage = null
                                resetIsError = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isResetLoading
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    if (resetEmail.isBlank()) {
                                        resetMessage = "Please enter your email address."
                                        resetIsError = true
                                        return@launch
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                                        resetMessage = "Please enter a valid email address."
                                        resetIsError = true
                                        return@launch
                                    }
                                    isResetLoading = true
                                    resetMessage = null
                                    handlePasswordReset(
                                        auth = auth,
                                        email = resetEmail.trim(),
                                        onSuccess = {
                                            resetMessage = "Password reset email sent! Check your inbox."
                                            resetIsError = false
                                            isResetLoading = false
                                        },
                                        onError = { error ->
                                            resetMessage = error
                                            resetIsError = true
                                            isResetLoading = false
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluePrimary
                            ),
                            enabled = !isResetLoading
                        ) {
                            if (isResetLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = BlueOnPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Send Link", color = BlueOnPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================================
    // EMAIL VERIFICATION DIALOG
    // ========================================
    if (showVerificationDialog) {
        Dialog(onDismissRequest = {
            if (!isResendingVerification) {
                showVerificationDialog = false
                resendMessage = null
            }
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Verify Your Email",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "A verification link has been sent to:\n$email\n\nPlease check your inbox and verify your email before logging in.",
                        fontSize = 14.sp,
                        color = OnSurfaceLight.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (resendMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = resendMessage!!,
                                fontSize = 13.sp,
                                color = OnSurfaceLight,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showVerificationDialog = false
                                resendMessage = null
                                // Switch to login mode so they can sign in after verifying
                                isSignUp = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isResendingVerification
                        ) {
                            Text("OK")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isResendingVerification = true
                                    resendMessage = null
                                    try {
                                        auth.currentUser?.sendEmailVerification()?.await()
                                        resendMessage = "Verification email resent!"
                                    } catch (e: Exception) {
                                        resendMessage = "Failed to resend. Please try again."
                                    } finally {
                                        isResendingVerification = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            enabled = !isResendingVerification
                        ) {
                            if (isResendingVerification) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = BlueOnPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Resend", color = BlueOnPrimary)
                            }
                        }
                    }
                }
            }
        }
    }


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

            // Forgot password link (only on login screen)
            if (!isSignUp) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(
                        onClick = {
                            resetEmail = email // pre-fill with whatever user typed
                            showForgotPassword = true
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = BlueOnPrimary.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

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
                                        Log.d("LoginScreen", "SignUp success, showing verification dialog...")
                                        isLoading = false
                                        showVerificationDialog = true
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
                                        if (error == "EMAIL_NOT_VERIFIED") {
                                            isLoading = false
                                            showVerificationDialog = true
                                        } else {
                                            errorMessage = error
                                        }
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
                    selectedRole = "member"
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

private suspend fun handlePasswordReset(
    auth: FirebaseAuth,
    email: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("LoginScreen", "Sending password reset email to: $email")
        auth.sendPasswordResetEmail(email).await()
        Log.d("LoginScreen", "Password reset email sent successfully")
        onSuccess()
    } catch (e: FirebaseAuthInvalidUserException) {
        Log.e("LoginScreen", "No user found for reset email", e)
        // Intentionally vague for security — don't confirm whether email exists
        onSuccess()
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Log.e("LoginScreen", "Invalid email for reset", e)
        onError("Please enter a valid email address.")
    } catch (e: Exception) {
        Log.e("LoginScreen", "Password reset failed", e)
        onError(e.message ?: "Failed to send reset email. Please try again.")
    }
}

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

        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user

        if (user == null) {
            Log.e("LoginScreen", "Auth returned null user")
            onError("Login failed. Please try again.")
            return
        }

        Log.d("LoginScreen", "Auth success, UID: ${user.uid}")

        // Block login if email is not verified
        if (!user.isEmailVerified) {
            Log.w("LoginScreen", "Email not verified for: ${user.email}")
            auth.signOut()
            onError("EMAIL_NOT_VERIFIED")
            return
        }

        val profileDoc = firestore.collection("users").document(user.uid).get().await()

        Log.d("LoginScreen", "Profile exists: ${profileDoc.exists()}")

        if (!profileDoc.exists()) {
            Log.d("LoginScreen", "Creating missing profile...")
            createUserProfile(user.uid, email, "User", "member", firestore)
        }

        Log.d("LoginScreen", "Loading profile into session...")
        val result = UserSessionManager.loadUserProfile(user.uid, firestore)

        if (result.isFailure) {
            Log.e("LoginScreen", "Failed to load profile", result.exceptionOrNull())
            onError("Failed to load user data. Please try again.")
            return
        }

        Log.d("LoginScreen", "Profile loaded successfully")
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

        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user

        if (user == null) {
            Log.e("LoginScreen", "Auth returned null user")
            onError("Sign up failed. Please try again.")
            return
        }

        Log.d("LoginScreen", "Auth account created, UID: ${user.uid}")

        Log.d("LoginScreen", "Creating Firestore profile with role: $role")
        createUserProfile(user.uid, email, name, role, firestore)

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

        // Send verification email — user must verify before logging in
        try {
            user.sendEmailVerification().await()
            Log.d("LoginScreen", "Verification email sent to: ${user.email}")
        } catch (e: Exception) {
            Log.w("LoginScreen", "Failed to send verification email", e)
            // Non-fatal: account is created, verification email can be resent
        }

        // Sign out immediately — force them to verify before accessing the app
        auth.signOut()
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