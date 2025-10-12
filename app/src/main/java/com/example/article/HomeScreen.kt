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
import com.example.article.ui.theme.*

data class Post(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val author: String = "",
    val timestamp: String = "",
    val likes: Int = 0,
    val comments: Int = 0,
    val category: String = "General"
)

@Composable
fun HomeScreen() {
    var posts by remember {
        mutableStateOf(
            listOf(
                Post(1, "🚀 Welcome to Forge!", "Discover amazing content and connect with creative minds from around the world.", "Admin", "2 hours ago", 42, 12, "Announcement"),
                Post(2, "✨ The Future of Design", "Exploring new trends in UI/UX design and how they're shaping the digital landscape.", "Designer", "4 hours ago", 28, 8, "Design"),
                Post(3, "🎯 Productivity Tips", "5 proven methods to boost your daily productivity and get more done.", "ProductivityGuru", "6 hours ago", 67, 23, "Tips")
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderMist)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // Header section
            item {
                WelcomeHeader()
            }

            // Posts
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onLike = { postId ->
                        posts = posts.map {
                            if (it.id == postId) it.copy(likes = it.likes + 1)
                            else it
                        }
                    },
                    onComment = { postId ->
                        posts = posts.map {
                            if (it.id == postId) it.copy(comments = it.comments + 1)
                            else it
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WelcomeHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RoyalViolet),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "📰 Latest Posts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightWhite
                )
                Text(
                    "Discover what's trending",
                    fontSize = 14.sp,
                    color = PeachGlow
                )
            }

            IconButton(
                onClick = { /* Handle refresh */ },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BrightWhite.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = BrightWhite
                )
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLike: (Int) -> Unit,
    onComment: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrightWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RoyalViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.author.first().uppercaseChar().toString(),
                            color = BrightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            post.author,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepPlum,
                            fontSize = 14.sp
                        )
                        Text(
                            post.timestamp,
                            color = SteelGray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Category badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = SoftLilac),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        post.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepPlum
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPlum
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                post.content,
                fontSize = 14.sp,
                color = SteelGray,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (post.content.length > 50) {
                Text(
                    text = if (isExpanded) "Show less" else "Read more",
                    color = RoyalViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { isExpanded = !isExpanded }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    // Like button
                    Row(
                        modifier = Modifier
                            .clickable {
                                isLiked = !isLiked
                                if (isLiked) onLike(post.id)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else SteelGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${post.likes + if (isLiked) 1 else 0}",
                            fontSize = 12.sp,
                            color = SteelGray
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Comment button
                    Row(
                        modifier = Modifier
                            .clickable { onComment(post.id) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Comment,
                            contentDescription = "Comment",
                            tint = SteelGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${post.comments}",
                            fontSize = 12.sp,
                            color = SteelGray
                        )
                    }
                }

                // Share button
                IconButton(onClick = { /* Handle share */ }) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = SteelGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
