package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
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
import com.example.article.core.UiState
import com.example.article.feed.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    userNeighborhood: String = "Your Neighborhood"
) {
    val uiState by viewModel.uiState.collectAsState()
    var unreadNotifications by remember { mutableIntStateOf(3) }

    // Fetch actual user neighborhood - FIXED
    var actualNeighborhood by remember { mutableStateOf(userNeighborhood) }

    LaunchedEffect(Unit) {
        viewModel.loadFeed()

        // Fetch user's neighborhood - COMPLETELY FIXED
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .addSnapshotListener { document, error ->
                    if (error != null) return@addSnapshotListener

                    if (document != null && document.exists()) {
                        // Try BOTH spellings
                        val hood = document.getString("neighborhood")
                            ?: document.getString("neighbourhood")

                        if (!hood.isNullOrBlank()) {
                            actualNeighborhood = hood
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Article",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
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
                        elevation = 6.dp,
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.4f)
                    )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
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
                        Surface(
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = Color(0xFFD32F2F)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Unable to load feed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.refreshFeed() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF42A5F5)
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                is UiState.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    FeedList(
                        feed = state.data as List<FeedItem>,
                        navController = navController,
                        userNeighborhood = actualNeighborhood,
                        onLoadMore = { viewModel.loadMore() },
                        onLike = { post ->
                            viewModel.toggleLike(post.id, post.likedByMe)
                        },
                        onDeletePost = { viewModel.deletePost(it) },
                        onDeleteAnnouncement = { viewModel.deleteAnnouncement(it) },
                        onReportPost = { postId ->
                            // Report post to Firestore
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                            if (currentUserId != null) {
                                FirebaseFirestore.getInstance()
                                    .collection("reports")
                                    .add(
                                        mapOf(
                                            "postId" to postId,
                                            "reportedBy" to currentUserId,
                                            "reportedAt" to System.currentTimeMillis(),
                                            "status" to "pending",
                                            "type" to "post"
                                        )
                                    )
                            }
                        }
                    )
                }

                UiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF42A5F5)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun FeedList(
    feed: List<FeedItem>,
    navController: NavController,
    userNeighborhood: String,
    onLoadMore: () -> Unit,
    onLike: (FeedItem.Post) -> Unit,
    onDeletePost: (String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onReportPost: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") {
            HomeHeader(userNeighborhood)
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF999999).copy(alpha = 0.6f)
                        )
                        Text(
                            "No posts yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1a1a1a)
                        )
                        Text(
                            "Be the first to share with your community!",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        } else {
            items(posts, key = { "post_${it.id}" }) { post ->
                PostCard(post, onLike, navController, onDeletePost, onReportPost)
            }
        }

        item(key = "pagination") {
            LaunchedEffect(feed.size) { onLoadMore() }
        }
    }
}

@Composable
private fun HomeHeader(userNeighborhood: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
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
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Good day 👋",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = userNeighborhood,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                }
                Text(
                    text = "Stay updated with your community",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
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

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF42A5F5).copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE3F2FD) // Light blue matching app theme
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFBBDEFB).copy(alpha = 0.3f),
                            Color(0xFFE3F2FD)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            // Header with icon - Compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFF42A5F5).copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF1976D2)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF42A5F5).copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "ANNOUNCEMENT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            letterSpacing = 0.4.sp
                        )
                    }
                    Text(
                        text = formatTimestamp(announcement.time),
                        fontSize = 11.sp,
                        color = Color(0xFF1976D2)
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
                        tint = Color(0xFF1976D2)
                    )
                }

                DropdownMenu(showMenu, { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title - Smaller font
            Text(
                text = announcement.title,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1),
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(6.dp))

            // Message - Compact
            Text(
                text = announcement.message,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF1565C0),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Announcement", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this announcement? This action cannot be undone.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(announcement.id)
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun PostCard(
    item: FeedItem.Post,
    onLike: (FeedItem.Post) -> Unit,
    navController: NavController,
    onDelete: (String) -> Unit,
    onReport: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val currentUserRole = UserSessionManager.currentUser.collectAsState().value?.role
    val canDelete = currentUserId == item.authorId || currentUserRole == "admin"

    val userName = item.author
    val userPhotoUrl = item.authorPhotoUrl

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFF37474F).copy(alpha = 0.4f),
                ambientColor = Color(0xFF546E7A).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header - Author Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with profile picture
                if (!userPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color(0xFF42A5F5).copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userName.firstOrNull()?.uppercase() ?: "?",
                                color = Color(0xFF42A5F5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // Author Name & Time
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = userName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1a1a1a)
                    )
                    Text(
                        text = formatTimestamp(item.time),
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }

                // More Options
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(showMenu, { showMenu = false }) {
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text("Delete", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Report", fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showMenu = false
                            showReportDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = Color(0xFFFF6F00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            // Content Text
            Text(
                text = item.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color(0xFF1a1a1a),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            // Image (if present)
            if (!item.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 3.dp
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = item.imageUrl,
                            error = rememberAsyncImagePainter(android.R.drawable.ic_menu_gallery)
                        ),
                        contentDescription = "Post image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0),
                thickness = 1.dp
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Like Button
                Surface(
                    onClick = { onLike(item) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.likedByMe)
                        Color(0xFF42A5F5).copy(alpha = 0.12f)
                    else
                        Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (item.likedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            modifier = Modifier.size(20.dp),
                            tint = if (item.likedByMe)
                                Color(0xFF42A5F5)
                            else
                                Color(0xFF666666)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (item.likes > 0) item.likes.toString() else "Like",
                            fontSize = 14.sp,
                            fontWeight = if (item.likedByMe) FontWeight.Bold else FontWeight.Medium,
                            color = if (item.likedByMe)
                                Color(0xFF42A5F5)
                            else
                                Color(0xFF666666)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Comment Button
                Surface(
                    onClick = { navController.navigate("comments/${item.id}/${item.authorId}") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Comment,
                            contentDescription = "Comment",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF666666)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (item.commentCount > 0) item.commentCount.toString() else "Comment",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(item.id)
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = Color(0xFFFF6F00),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Report Post", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to report this post? Our moderation team will review it.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        onReport(item.id)
                    }
                ) {
                    Text("Report", color = Color(0xFFFF6F00), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}