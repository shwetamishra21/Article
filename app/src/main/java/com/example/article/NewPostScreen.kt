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
import com.example.article.Repository.PostType
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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    onPostUploaded: () -> Unit,
    feedViewModel: HomeViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(PostType.POST) }
    var content by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) }

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
                        fontSize = 20.sp,
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
                        elevation = 6.dp,
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.4f)
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
                            Color(0xFF42A5F5).copy(alpha = 0.05f),
                            Color(0xFFF5F5F5)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Type Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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

            // Announcement Title
            if (selectedType == PostType.ANNOUNCEMENT) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                        placeholder = { Text("Enter announcement title", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }
            }

            // Content
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                    placeholder = {
                        Text(
                            if (selectedType == PostType.POST)
                                "Share something with your neighbors..."
                            else
                                "Write announcement details...",
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .padding(4.dp),
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF42A5F5),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }

            // Image Upload (POST ONLY)
            if (selectedType == PostType.POST) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Attach Image (Optional)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1a1a1a)
                    )

                    if (imageUri != null) {
                        // Image Preview
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
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
                                        .padding(12.dp)
                                        .size(36.dp)
                                        .shadow(
                                            elevation = 4.dp,
                                            shape = CircleShape
                                        )
                                        .background(
                                            Color.White,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Upload Progress
                        if (loading && uploadProgress > 0f) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF42A5F5)
                                    )
                                    LinearProgressIndicator(
                                        progress = uploadProgress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF42A5F5),
                                        trackColor = Color(0xFF42A5F5).copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Upload Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clickable { imagePicker.launch("image/*") },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 2.dp,
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
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF42A5F5).copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Tap to add image",
                                    fontSize = 15.sp,
                                    color = Color(0xFF42A5F5),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "High quality upload",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Error Message
            error?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = it,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Submit Button
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
                    uploadProgress = 0f

                    scope.launch {
                        try {
                            val postId = UUID.randomUUID().toString()
                            var imageUrl: String? = null

                            // Upload image if present - HIGH QUALITY
                            if (imageUri != null && selectedType == PostType.POST) {
                                try {
                                    imageUrl = CloudinaryHelper.uploadPostImage(
                                        uri = imageUri!!,
                                        userId = user.uid,
                                        onProgress = { progress ->
                                            uploadProgress = progress
                                        }
                                    )
                                } catch (e: Exception) {
                                    error = "Image upload failed: ${e.localizedMessage}"
                                    loading = false
                                    return@launch
                                }
                            }

                            // Create post document
                            val postData = hashMapOf(
                                "type" to if (selectedType == PostType.POST) "post" else "announcement",
                                "content" to content,
                                "authorId" to user.uid,
                                "authorName" to (user.email ?: "User"),
                                "likes" to 0,
                                "likedBy" to emptyMap<String, Boolean>(),
                                "commentCount" to 0,
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )

                            // Add type-specific fields
                            if (selectedType == PostType.ANNOUNCEMENT) {
                                postData["title"] = title
                            }

                            if (imageUrl != null) {
                                postData["imageUrl"] = imageUrl
                            }

                            // Save to Firestore
                            firestore.collection("posts")
                                .document(postId)
                                .set(postData)
                                .await()

                            // Add optimistic item to feed
                            val newItem = if (selectedType == PostType.POST) {
                                com.example.article.FeedItem.Post(
                                    id = postId,
                                    author = user.email ?: "You",
                                    content = content,
                                    time = System.currentTimeMillis(),
                                    likes = 0,
                                    commentCount = 0,
                                    likedByMe = false,
                                    imageUrl = imageUrl
                                )
                            } else {
                                com.example.article.FeedItem.Announcement(
                                    id = postId,
                                    title = title,
                                    message = content,
                                    time = System.currentTimeMillis()
                                )
                            }

                            feedViewModel.addOptimistic(newItem)

                            loading = false
                            onPostUploaded()

                        } catch (e: Exception) {
                            loading = false
                            uploadProgress = 0f
                            error = e.localizedMessage ?: "Failed to post"
                        }
                    }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = if (isEnabled) 6.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.4f)
                    ),
                shape = RoundedCornerShape(16.dp),
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                            Text(
                                text = if (uploadProgress > 0f)
                                    "Uploading ${(uploadProgress * 100).toInt()}%"
                                else "Posting...",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (selectedType == PostType.POST) Icons.Default.Send
                                else Icons.Default.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isEnabled) Color.White else Color(0xFF999999)
                            )
                            Text(
                                text = if (selectedType == PostType.POST) "Post" else "Publish",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isEnabled) Color.White else Color(0xFF999999)
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
        shape = RoundedCornerShape(12.dp),
        color = if (selected)
            Color(0xFF42A5F5).copy(alpha = 0.15f)
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) Color(0xFF42A5F5) else Color(0xFF888888)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF42A5F5) else Color(0xFF888888)
            )
        }
    }
}