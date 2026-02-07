package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.article.Repository.LikeRepository
import com.example.article.core.UiState
import com.example.article.feed.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var unreadNotifications by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Article",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    // ✨ NOTIFICATIONS WITH BADGE
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge(
                                    containerColor = Color(0xFFFF5252),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        "$unreadNotifications",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { unreadNotifications = 0 }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
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
        Box(
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
        ) {
            when (val state = uiState) {
                UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp),
                            color = Color(0xFF42A5F5)
                        )
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⚠️", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Unable to load feed",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.refreshFeed() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry", fontSize = 13.sp)
                        }
                    }
                }

                is UiState.Success -> {
                    FeedList(
                        feed = state.data,
                        navController = navController,
                        onLoadMore = { viewModel.loadMore() },
                        onLike = { post ->
                            viewModel.toggleLikeOptimistic(post.id)
                            LikeRepository.toggleLike(post.id, post.likedByMe)
                        },
                        onDeletePost = { viewModel.deletePost(it) },
                        onDeleteAnnouncement = { viewModel.deleteAnnouncement(it) }
                    )
                }

                UiState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun FeedList(
    feed: List<FeedItem>,
    navController: NavController,
    onLoadMore: () -> Unit,
    onLike: (FeedItem.Post) -> Unit,
    onDeletePost: (String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            HomeHeader()
        }

        val announcements = feed.filterIsInstance<FeedItem.Announcement>()
        if (announcements.isNotEmpty()) {
            items(announcements, key = { "announcement_${it.id}" }) { announcement ->
                AnnouncementCard(announcement, onDeleteAnnouncement)
            }
        }

        val posts = feed.filterIsInstance<FeedItem.Post>()
        if (posts.isEmpty() && announcements.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "📝", fontSize = 36.sp)
                        Text(
                            "No posts yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Be the first to share!",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        } else {
            items(posts, key = { "post_${it.id}" }) { post ->
                PostCard(post, onLike, navController, onDeletePost)
            }
        }

        item(key = "pagination") {
            LaunchedEffect(feed.size) { onLoadMore() }
        }
    }
}

@Composable
private fun HomeHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF42A5F5),
                            Color(0xFF4DD0E1)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Good day 👋",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Your Neighborhood",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Stay updated with your community",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: FeedItem.Announcement,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF42A5F5).copy(alpha = 0.20f), // ✨ INCREASED TINT
                            Color(0xFF4DD0E1).copy(alpha = 0.14f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = announcement.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp, // ✨ REDUCED FONT
                            color = Color(0xFF1a1a1a),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF666666)
                        )
                    }

                    DropdownMenu(showMenu, { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete", fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFB71C1C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Text(
                    text = announcement.message,
                    fontSize = 12.sp, // ✨ REDUCED FONT
                    lineHeight = 17.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Announcement", fontSize = 16.sp) },
            text = { Text("Are you sure?", fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(announcement.id)
                    }
                ) {
                    Text("Delete", color = Color(0xFFB71C1C), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
private fun PostCard(
    item: FeedItem.Post,
    onLike: (FeedItem.Post) -> Unit,
    navController: NavController,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF42A5F5).copy(alpha = 0.15f),
                                        Color(0xFF4DD0E1).copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.author.firstOrNull()?.uppercase() ?: "?",
                            color = Color(0xFF42A5F5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Column {
                        Text(
                            item.author,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp // ✨ REDUCED FONT
                        )
                        Text(
                            "Just now",
                            fontSize = 11.sp, // ✨ REDUCED FONT
                            color = Color(0xFF666666)
                        )
                    }
                }

                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF666666)
                    )
                }

                DropdownMenu(showMenu, { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFB71C1C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            Text(
                text = item.content,
                fontSize = 13.sp, // ✨ REDUCED FONT
                lineHeight = 18.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF1a1a1a)
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFF42A5F5).copy(alpha = 0.15f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { onLike(item) },
                    enabled = !item.likedByMe,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (item.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (item.likedByMe) Color(0xFF42A5F5) else Color(0xFF666666),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (item.likes > 0) {
                    Text(
                        "${item.likes}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = { navController.navigate("comments/${item.id}") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    "${item.commentCount}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post", fontSize = 16.sp) },
            text = { Text("Are you sure?", fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(item.id)
                    }
                ) {
                    Text("Delete", color = Color(0xFFB71C1C), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }
}