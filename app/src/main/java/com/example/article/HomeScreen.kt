package com.example.article

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
import com.example.article.ui.theme.*

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

@Composable
fun HomeScreen() {
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
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item { HomeHeader() }

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

@Composable
fun HomeHeader() {
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
                "🔥 Forge Feed",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Discover the latest ideas and trends",
                color = PeachGlow,
                fontSize = 14.sp
            )
        }
    }
}

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
                    Icon(Icons.Filled.Comment, contentDescription = null)
                }
            }
        }
    }
}
