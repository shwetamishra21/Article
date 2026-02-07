package com.example.article

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.article.feed.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

enum class PostType { POST, ANNOUNCEMENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    onPostUploaded: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var selectedType by remember { mutableStateOf(PostType.POST) }
    var content by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Post",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPostUploaded) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF42A5F5),
                                Color(0xFF4DD0E1)
                            )
                        )
                    )
                    .shadow(
                        elevation = 4.dp,
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
                    )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF42A5F5).copy(alpha = 0.03f),
                            Color(0xFFFAFAFA)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ✨ TYPE TOGGLE
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PostTypeTab(
                        text = "Post",
                        icon = Icons.Default.Article,
                        selected = selectedType == PostType.POST,
                        onClick = { selectedType = PostType.POST },
                        modifier = Modifier.weight(1f)
                    )
                    PostTypeTab(
                        text = "Announcement",
                        icon = Icons.Default.Campaign,
                        selected = selectedType == PostType.ANNOUNCEMENT,
                        onClick = { selectedType = PostType.ANNOUNCEMENT },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ✨ ANNOUNCEMENT TITLE
            if (selectedType == PostType.ANNOUNCEMENT) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontSize = 13.sp) },
                    placeholder = { Text("Enter announcement title", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF42A5F5),
                        unfocusedBorderColor = Color(0xFF42A5F5).copy(alpha = 0.2f)
                    )
                )
            }

            // ✨ CONTENT
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content", fontSize = 13.sp) },
                placeholder = {
                    Text(
                        if (selectedType == PostType.POST)
                            "Share something with your neighbors..."
                        else
                            "Write announcement details...",
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                maxLines = 8,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF42A5F5),
                    unfocusedBorderColor = Color(0xFF42A5F5).copy(alpha = 0.2f)
                )
            )

            // ✨ IMAGE UPLOAD (POST ONLY)
            if (selectedType == PostType.POST) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Attach Image (Optional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1a1a1a)
                    )

                    if (imageUri != null) {
                        // Image Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Remove button
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        // Upload Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable { imagePicker.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF42A5F5).copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = Color(0xFF42A5F5).copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF42A5F5).copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Tap to add image",
                                    fontSize = 13.sp,
                                    color = Color(0xFF42A5F5),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ✨ ERROR MESSAGE
            error?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFB71C1C).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = it,
                            color = Color(0xFFB71C1C),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ✨ SUBMIT BUTTON
            val isEnabled = !loading && content.isNotBlank() &&
                    (selectedType == PostType.POST || title.isNotBlank())

            Button(
                onClick = {
                    val user = auth.currentUser
                    if (user == null) {
                        error = "You must be logged in"
                        return@Button
                    }

                    if (selectedType == PostType.ANNOUNCEMENT && title.isBlank()) {
                        error = "Title required for announcements"
                        return@Button
                    }

                    loading = true
                    error = null

                    val id = UUID.randomUUID().toString()
                    val timestamp = System.currentTimeMillis()

                    // Optimistic UI
                    when (selectedType) {
                        PostType.POST -> {
                            viewModel.addOptimistic(
                                FeedItem.Post(
                                    id = id,
                                    author = user.email ?: "You",
                                    content = content,
                                    time = timestamp,
                                    likes = 0,
                                    commentCount = 0,
                                    likedByMe = false
                                )
                            )
                        }
                        PostType.ANNOUNCEMENT -> {
                            viewModel.addOptimistic(
                                FeedItem.Announcement(
                                    id = id,
                                    title = title,
                                    message = content,
                                    time = timestamp
                                )
                            )
                        }
                    }

                    // Firestore write
                    val postData = hashMapOf(
                        "type" to selectedType.name.lowercase(),
                        "content" to content,
                        "title" to if (selectedType == PostType.ANNOUNCEMENT) title else "",
                        "authorName" to (user.email ?: "User"),
                        "authorId" to user.uid,
                        "likes" to 0,
                        "commentCount" to 0,
                        "hasImage" to (imageUri != null),
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("posts")
                        .document(id)
                        .set(postData)
                        .addOnSuccessListener {
                            loading = false
                            onPostUploaded()
                        }
                        .addOnFailureListener { exception ->
                            loading = false
                            error = exception.localizedMessage ?: "Failed to post"
                        }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = if (isEnabled) {
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF42A5F5),
                                        Color(0xFF4DD0E1)
                                    )
                                )
                            )
                    } else Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (selectedType == PostType.POST) Icons.Default.Send else Icons.Default.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isEnabled) Color.White else Color(0xFF666666)
                            )
                            Text(
                                text = if (selectedType == PostType.POST) "Post" else "Publish",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (isEnabled) Color.White else Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostTypeTab(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (selected)
            Color(0xFF42A5F5).copy(alpha = 0.15f)
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) Color(0xFF42A5F5) else Color(0xFF666666)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFF42A5F5) else Color(0xFF666666)
            )
        }
    }
}