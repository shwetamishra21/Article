package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.FeedItem
import com.example.article.feed.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

enum class PostType { POST, ANNOUNCEMENT }

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
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val bgGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(16.dp)
    ) {

        Text("Create", fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))

        /* ---------- TYPE TOGGLE ---------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(6.dp)
        ) {
            PostTypeTab(
                text = "Post",
                selected = selectedType == PostType.POST,
                onClick = { selectedType = PostType.POST },
                modifier = Modifier.weight(1f)
            )
            PostTypeTab(
                text = "Announcement",
                selected = selectedType == PostType.ANNOUNCEMENT,
                onClick = { selectedType = PostType.ANNOUNCEMENT },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        /* ---------- ANNOUNCEMENT TITLE ---------- */
        if (selectedType == PostType.ANNOUNCEMENT) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Announcement title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
        }

        /* ---------- CONTENT ---------- */
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = {
                Text(
                    if (selectedType == PostType.POST)
                        "Write something for your neighbors…"
                    else
                        "Write announcement details…"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))

        /* ---------- SUBMIT ---------- */
        Button(
            enabled = !loading && content.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = {

                val user = auth.currentUser
                if (user == null) {
                    error = "Not authenticated"
                    return@Button
                }

                if (selectedType == PostType.ANNOUNCEMENT && title.isBlank()) {
                    error = "Title required for announcement"
                    return@Button
                }

                loading = true
                error = null

                val id = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()

                /* ---------- OPTIMISTIC UI ---------- */
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

                /* ---------- FIRESTORE WRITE ---------- */
                val postData = mapOf(
                    "type" to selectedType.name.lowercase(),
                    "content" to content,
                    "title" to title,
                    "authorName" to (user.email ?: "User"),
                    "authorId" to user.uid,
                    "likes" to 0,
                    "commentCount" to 0,
                    "createdAt" to timestamp
                )

                firestore.collection("posts")
                    .document(id)
                    .set(postData)
                    .addOnCompleteListener {
                        loading = false
                        onPostUploaded()
                    }
            }
        ) {
            Icon(
                if (selectedType == PostType.POST)
                    Icons.Default.Image
                else
                    Icons.Default.Campaign,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (selectedType == PostType.POST) "Post" else "Publish Announcement")
        }
    }
}

/* ---------- TAB ---------- */

@Composable
private fun PostTypeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

}
