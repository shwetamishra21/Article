package com.example.article

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    val firestore = Firebase.firestore
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var bio by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    // Fetch user data from Firestore on launch
    LaunchedEffect(Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    onNameChange(document.getString("name") ?: "")
                    bio = document.getString("bio") ?: ""
                    profileImageUrl = document.getString("profileImage") ?: ""
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileScreen", "Error fetching user data", e)
            }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUrl = it.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Profile Picture ---
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    if (profileImageUrl.isNotEmpty()) profileImageUrl
                    else "https://cdn-icons-png.flaticon.com/512/149/149071.png"
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Name Field ---
        if (isEditing) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    if (name.isEmpty())
                        Text(
                            "Enter your name",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    innerTextField()
                }
            )
        } else {
            Text(
                text = if (name.isNotEmpty()) name else "Unnamed User",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Bio Field ---
        if (isEditing) {
            BasicTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    if (bio.isEmpty())
                        Text(
                            "Enter your bio",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    innerTextField()
                }
            )
        } else {
            Text(
                text = if (bio.isNotEmpty()) bio else "No bio added yet.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isEditing) {
                Button(
                    onClick = {
                        val userData = mapOf(
                            "name" to name,
                            "bio" to bio,
                            "profileImage" to profileImageUrl
                        )
                        firestore.collection("users").document(userId)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d("ProfileScreen", "Profile updated successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileScreen", "Error updating profile", e)
                            }
                        isEditing = false
                    }
                ) {
                    Text("Save")
                }

                OutlinedButton(onClick = { isEditing = false }) {
                    Text("Cancel")
                }
            } else {
                Button(onClick = { isEditing = true }) {
                    Text("Edit Profile")
                }

                OutlinedButton(onClick = onLogout) {
                    Text("Logout")
                }
            }
        }
    }
}
