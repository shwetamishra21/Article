package com.example.article

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import coil.compose.AsyncImage
import com.example.article.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// -------------------------------
// 🔹 DATA CLASS FOR POSTS
// -------------------------------
data class Post(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val timestamp: String,
    val likes: Int,
    val comments: Int,
    val category: String
)

// -------------------------------
// 🔹 HOME SCREEN
// -------------------------------
@Composable
fun HomeScreen() {
    val auth = FirebaseAuth.getInstance()
    val userEmail = auth.currentUser?.email ?: "User"
    val username = userEmail.substringBefore("@")

    var posts by remember {
        mutableStateOf(
            listOf(
                Post(1, "🚀 Welcome to Forge!", "Discover amazing content and connect with creative minds.", "Admin", "2h ago", 42, 12, "Announcement"),
                Post(2, "✨ The Future of Design", "Exploring trends in UI/UX shaping digital experiences.", "Designer", "4h ago", 28, 8, "Design"),
                Post(3, "🎯 Productivity Hacks", "Boost your daily workflow with 5 simple tips.", "Guru", "6h ago", 67, 23, "Tips")
            )
        )
    }

    val bgColor = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                HomeHeader(username = username)
            }

            items(posts, key = { it.id }) { post ->
                PostCard(post, onLike = { postId ->
                    posts = posts.map {
                        if (it.id == postId) it.copy(likes = it.likes + 1) else it
                    }
                })
            }
        }
    }
}

// -------------------------------
// 🔹 HEADER WITH GREETING
// -------------------------------
@Composable
fun HomeHeader(username: String) {
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                "Hey $username! 👋",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Discover the latest ideas and trends",
                color = PeachGlow,
                fontSize = 14.sp
            )
        }
    }
}

// -------------------------------
// 🔹 POST CARD
// -------------------------------
@Composable
fun PostCard(post: Post, onLike: (Int) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) CardGlassDark else CardGlassLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.author.first().uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(post.author, fontWeight = FontWeight.Bold)
                    Text(post.timestamp, fontSize = 12.sp, color = SteelGray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                post.content,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = {
                    isLiked = !isLiked
                    if (isLiked) onLike(post.id)
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                }
            }
        }
    }
}

// -------------------------------
// 🔹 NEW POST SCREEN
// -------------------------------
@Composable
fun NewPostScreen(onPostUploaded: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📸 Create a Post", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
                .background(Color(0xFFF3F3F3), RoundedCornerShape(12.dp))
                .clickable { imagePicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = null)
            } else {
                Text("Tap to choose image")
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            placeholder = { Text("Write a caption...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        ForgeButton(
            text = if (isUploading) "Uploading..." else "Upload Post",
            onClick = {
                val user = auth.currentUser
                val uri = imageUri
                if (user == null) { errorMessage = "Please log in first"; return@ForgeButton }
                if (uri == null) { errorMessage = "Select an image"; return@ForgeButton }

                isUploading = true
                errorMessage = null

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val ref = storage.reference.child("posts/${user.uid}/${System.currentTimeMillis()}.jpg")
                        ref.putFile(uri).await()
                        val url = ref.downloadUrl.await().toString()

                        val post = hashMapOf(
                            "authorId" to user.uid,
                            "authorName" to (user.displayName ?: user.email ?: "Anonymous"),
                            "caption" to caption,
                            "imageUrl" to url,
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("posts").add(post).await()

                        CoroutineScope(Dispatchers.Main).launch {
                            isUploading = false
                            imageUri = null
                            caption = ""
                            onPostUploaded()
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            isUploading = false
                            errorMessage = e.localizedMessage
                        }
                    }
                }
            }
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage!!, color = Color.Red)
        }
    }
}
