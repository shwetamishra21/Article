package com.example.article.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx. compose. material3.TabRowDefaults. tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.article.Repository.AdminPost
import com.example.article.Repository.ContentModerationViewModel
import com.example.article.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentModerationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ContentModerationViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Reported", "All Posts")

    var showDeleteDialog by remember { mutableStateOf<AdminPost?>(null) }
    var showDetailsDialog by remember { mutableStateOf<AdminPost?>(null) }

    val dateFmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    LaunchedEffect(Unit) { viewModel.loadPosts() }
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }

    val filteredPosts = when (selectedTab) {
        0 -> posts.filter { it.isReported }
        else -> posts
    }
    val reportedCount = posts.count { it.isReported }

    showDeleteDialog?.let { post ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFCC4444)) },
            title = { Text("Remove Post?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Post by ${post.authorName} will be permanently deleted.")
                    Text(
                        "\"${post.content.take(80)}${if (post.content.length > 80) "…" else ""}\"",
                        fontSize = 12.sp, color = Color(0xFF888888), fontStyle = FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePost(post.id); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4444))
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }

    showDetailsDialog?.let { post ->
        val tsLabel = if (post.timestamp > 0L) dateFmt.format(Date(post.timestamp)) else "Unknown"
        AlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = { Text("Post Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Author: ${post.authorName}", fontWeight = FontWeight.SemiBold)
                    Text("Posted: $tsLabel", fontSize = 13.sp)
                    if (post.isReported) {
                        Text("Reports: ${post.reportCount}", color = Color(0xFFCC4444), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(post.content, fontSize = 13.sp, color = Color(0xFF555555))
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = null }) { Text("Close") }
            },
            dismissButton = if (post.isReported) {
                {
                    TextButton(onClick = {
                        viewModel.dismissReport(post)
                        showDetailsDialog = null
                    }) { Text("Dismiss Report") }
                }
            } else null
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Content Moderation", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text(
                            "${posts.size} post${if (posts.size != 1) "s" else ""}${if (reportedCount > 0) " · $reportedCount reported" else ""}",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPosts() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(Brush.linearGradient(colors = listOf(BluePrimary, BlueSecondary)))
                    .shadow(elevation = 6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BluePrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = BluePrimary,
                        unselectedContentColor = Color(0xFF999999),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal)
                                if (index == 0 && reportedCount > 0) {
                                    Surface(color = Color(0xFFCC4444), shape = CircleShape) {
                                        Text("$reportedCount", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            when {
                loading && posts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BluePrimary)
                    }
                }
                filteredPosts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                if (selectedTab == 0) Icons.Default.CheckCircle else Icons.Default.Article,
                                null,
                                modifier = Modifier.size(56.dp),
                                tint = if (selectedTab == 0) Color(0xFF4CAF50).copy(alpha = 0.7f) else BluePrimary.copy(alpha = 0.4f)
                            )
                            Text(
                                if (selectedTab == 0) "No reported posts" else "No posts yet",
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight
                            )
                            Text(
                                if (selectedTab == 0) "Everything looks clean!" else "Posts from members will appear here.",
                                fontSize = 13.sp, color = Color(0xFF888888)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                timestampLabel = if (post.timestamp > 0L) dateFmt.format(Date(post.timestamp)) else "",
                                onViewDetails = { showDetailsDialog = post },
                                onDelete = { showDeleteDialog = post },
                                onDismissReport = if (post.isReported) {
                                    { viewModel.dismissReport(post) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    post: AdminPost,
    timestampLabel: String,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
    onDismissReport: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (post.isReported) 4.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (post.isReported) Color(0xFFCC4444).copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (post.isReported) Color(0xFFFFF8F8) else SurfaceLight
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(BluePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.authorName.firstOrNull()?.uppercase() ?: "?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                    if (timestampLabel.isNotEmpty()) Text(timestampLabel, fontSize = 11.sp, color = Color(0xFF999999))
                }
                if (post.isReported) {
                    Surface(color = Color(0xFFCC4444).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Flag, null, modifier = Modifier.size(13.dp), tint = Color(0xFFCC4444))
                            Text("${post.reportCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCC4444))
                        }
                    }
                }
            }

            Text(post.content, fontSize = 13.sp, color = Color(0xFF444444), maxLines = 4, lineHeight = 19.sp)

            post.imageUrl?.let {
                AsyncImage(
                    model = it, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onViewDetails, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Details", fontSize = 12.sp)
                }
                if (onDismissReport != null) {
                    OutlinedButton(
                        onClick = onDismissReport, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FlagCircle, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dismiss", fontSize = 12.sp)
                    }
                }
                OutlinedButton(
                    onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCC4444).copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", fontSize = 12.sp)
                }
            }
        }
    }
}