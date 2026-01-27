package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var signUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val bgGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = if (signUp) "Create Account" else "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                text = if (signUp) "Join your neighborhood" else "Sign in to continue",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    null
                                )
                            }
                        },
                        visualTransformation =
                            if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        enabled = !loading,
                        onClick = {
                            error = null

                            if (email.isBlank() || password.isBlank()) {
                                error = "Please fill all fields"
                                return@Button
                            }

                            loading = true

                            if (signUp) {
                                // 🔹 SIGN UP
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { result ->
                                        val user = result.user
                                        if (user == null) {
                                            loading = false
                                            error = "Signup failed"
                                            return@addOnSuccessListener
                                        }

                                        val userDoc = mapOf(
                                            "email" to user.email,
                                            "role" to "member", // ✅ DEFAULT ROLE
                                            "createdAt" to System.currentTimeMillis()
                                        )

                                        firestore.collection("users")
                                            .document(user.uid)
                                            .set(userDoc)
                                            .addOnSuccessListener {
                                                loading = false
                                                onLoginSuccess()
                                            }
                                            .addOnFailureListener {
                                                // 🔒 FAIL-SAFE: do not block login
                                                loading = false
                                                onLoginSuccess()
                                            }
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        error = it.localizedMessage
                                    }

                            } else {
                                // 🔹 LOGIN
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        loading = false
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        error = it.localizedMessage
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            if (signUp) Icons.Default.PersonAdd else Icons.Default.Login,
                            null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (signUp) "Sign Up" else "Sign In")
                    }

                    TextButton(
                        onClick = { signUp = !signUp },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            if (signUp)
                                "Already have an account? Sign In"
                            else
                                "New here? Create an account"
                        )
                    }
                }
            }
        }
    }
}
