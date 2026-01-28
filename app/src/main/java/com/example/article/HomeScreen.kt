package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

/* ---------------- MODELS ---------------- */

data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val time: String
)

data class FeedPost(
    val id: String,
    val author: String,
    val content: String,
    val time: String,
    val likes: Int
)

/* ---------------- HOME SCREEN ---------------- */

@Composable
fun HomeScreen(
    role: String = "member" // ✅ SAFE DEFAULT
) {

    var announcements by remember {
        mutableStateOf(
            listOf(
                Announcement(
                    UUID.randomUUID().toString(),
                    "Water Supply Notice 🚰",
                    "Water supply will be unavailable tomorrow from 10 AM – 1 PM.",
                    "1h ago"
                )
            )
        )
    }

    var posts by remember {
        mutableStateOf(
            listOf(
                FeedPost(UUID.randomUUID().toString(), "Ravi", "Anyone knows a good electrician nearby?", "2h ago", 4),
                FeedPost(UUID.randomUUID().toString(), "You", "Evening walks are so peaceful lately 🌆", "5h ago", 9)
            )
        )
    }

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {

        /* ---------- HEADER ---------- */
        item {
            HomeHeader()
        }

        /* ---------- ADMIN ONLY (future use) ---------- */
        if (role == "admin") {
            item {
                SectionTitle("Admin Controls")
            }

            item {
                Text(
                    text = "You are viewing admin privileges",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        /* ---------- ANNOUNCEMENTS ---------- */
        if (announcements.isNotEmpty()) {
            item {
                SectionTitle("Announcements")
            }

            items(
                announcements,
                key = { it.id }
            ) { announcement ->
                AnnouncementCard(announcement)
            }
        }

        /* ---------- POSTS ---------- */
        item {
            SectionTitle("Community Posts")
        }

        items(
            posts,
            key = { it.id }
        ) { post ->
            PostCard(
                post = post,
                onLike = {
                    posts = posts.map {
                        if (it.id == post.id)
                            it.copy(likes = it.likes + 1)
                        else it
                    }
                }
            )
        }
    }
}

/* ---------------- HEADER ---------------- */

@Composable
private fun HomeHeader() {
    val gradient = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(gradient, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Good day 👋",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp
            )
            Text(
                text = "Your Neighborhood",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Stay updated with your community",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

/* ---------------- SECTION TITLE ---------------- */

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/* ---------------- ANNOUNCEMENT CARD ---------------- */

@Composable
private fun AnnouncementCard(item: Announcement) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(item.message, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                item.time,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ---------------- POST CARD ---------------- */

@Composable
private fun PostCard(
    post: FeedPost,
    onLike: () -> Unit
) {
    var liked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        post.author.first().uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(post.author, fontWeight = FontWeight.Bold)
                    Text(
                        post.time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                post.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    liked = !liked
                    if (liked) onLike()
                }) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null
                    )
                }

                IconButton(onClick = { }) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    "${post.likes}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
