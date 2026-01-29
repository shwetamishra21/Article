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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.FeedItem
import com.example.article.feed.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val feed by viewModel.feed.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    // Attach realtime listener once
    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item { HomeHeader() }

        if (loading && feed.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (!loading && feed.isEmpty()) {
            item {
                Text(
                    text = "No posts yet",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(
            items = feed,
            key = {
                when (it) {
                    is FeedItem.Post -> it.id
                    is FeedItem.Announcement -> it.id
                }
            }
        ) { item ->
            when (item) {
                is FeedItem.Announcement -> AnnouncementCard(item)
                is FeedItem.Post -> PostCard(item)
            }
        }

        // Pagination trigger
        item {
            LaunchedEffect(feed.size) {
                viewModel.loadMore()
            }
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
            Text("Good day 👋", color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp)
            Text(
                "Your Neighborhood",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Stay updated with your community",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

/* ---------------- ANNOUNCEMENT ---------------- */

@Composable
private fun AnnouncementCard(item: FeedItem.Announcement) {
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
        }
    }
}

/* ---------------- POST ---------------- */

@Composable
private fun PostCard(item: FeedItem.Post) {
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
                        item.author.first().uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))
                Text(item.author, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                item.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { liked = !liked }) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null
                    )
                }

                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    text = "${item.commentCount}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
