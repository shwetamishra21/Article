package com.example.article

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.article.Repository.ProfileViewModel
import com.example.article.Repository.ProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    role: UserRole,
    onLogout: () -> Unit,
    onCreatePost: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editNeighborhood by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadProfileImage(it,context) {
                selectedImageUri = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.Logout,
                            "Logout",
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
        when (val state = uiState) {
            ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF42A5F5))
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.padding(24.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color(0xFFD32F2F)
                            )
                            Text(
                                "Error loading profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                state.message,
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadProfile() },
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

            is ProfileUiState.Success -> {
                val profile = state.profile

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item(key = "header") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 3.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                // Profile Image
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .border(
                                            width = 4.dp,
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF42A5F5),
                                                    Color(0xFF4DD0E1)
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .clickable { imagePicker.launch("image/*") }
                                        .background(Color(0xFFF0F0F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profile.photoUrl.isNotEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(profile.photoUrl),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(52.dp),
                                            tint = Color(0xFF999999)
                                        )
                                    }

                                    if (isUpdating) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 3.dp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Name & Bio
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Name", fontSize = 14.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF42A5F5),
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = editNeighborhood,
                                        onValueChange = { editNeighborhood = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Neighborhood", fontSize = 14.sp) },
                                        placeholder = { Text("e.g., Downtown, West Side", fontSize = 14.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF42A5F5),
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        ),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFF42A5F5),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    )

                                    OutlinedTextField(
                                        value = editBio,
                                        onValueChange = { editBio = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Bio", fontSize = 14.sp) },
                                        maxLines = 3,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF42A5F5),
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        )
                                    )
                                } else {
                                    Text(
                                        text = profile.name.ifEmpty { "Set your name" },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1a1a1a)
                                    )

                                    if (profile.neighbourhood.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFF42A5F5)
                                            )
                                            Text(
                                                text = profile.neighbourhood,
                                                fontSize = 14.sp,
                                                color = Color(0xFF42A5F5),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Text(
                                        text = profile.bio.ifEmpty { "Add a bio" },
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }

                                // Role Badge
                                Surface(
                                    color = Color(0xFF42A5F5).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 18.dp,
                                            vertical = 10.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF42A5F5)
                                        )
                                        Text(
                                            text = role.name.lowercase().replace("_", " ")
                                                .replaceFirstChar { it.uppercase() },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF42A5F5)
                                        )
                                    }
                                }

                                // Edit/Save Buttons
                                if (isEditing) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { isEditing = false },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isUpdating,
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 2.dp,
                                                color = Color(0xFF42A5F5).copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text(
                                                "Cancel",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.updateProfile(editName, editBio, editNeighborhood) {
                                                    isEditing = false
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isUpdating,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF42A5F5)
                                            )
                                        ) {
                                            if (isUpdating) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.5.dp,
                                                    color = Color.White
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "Save",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            editName = profile.name
                                            editBio = profile.bio
                                            editNeighborhood = profile.neighbourhood
                                            isEditing = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF42A5F5)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Edit Profile",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (role != UserRole.SERVICE_PROVIDER) {
                                    OutlinedButton(
                                        onClick = onCreatePost,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 2.dp,
                                            color = Color(0xFF42A5F5)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color(0xFF42A5F5)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Create New Post",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF42A5F5)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "posts_header") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "My Posts (${state.posts.size})",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1a1a1a)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewMode = ViewMode.LIST },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ViewList,
                                            contentDescription = "List view",
                                            tint = if (viewMode == ViewMode.LIST)
                                                Color(0xFF42A5F5)
                                            else
                                                Color(0xFF999999)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewMode = ViewMode.GRID },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = "Grid view",
                                            tint = if (viewMode == ViewMode.GRID)
                                                Color(0xFF42A5F5)
                                            else
                                                Color(0xFF999999)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (state.posts.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
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
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        "Start sharing with your community",
                                        fontSize = 13.sp,
                                        color = Color(0xFF999999)
                                    )
                                }
                            }
                        }
                    } else if (viewMode == ViewMode.GRID) {
                        item(key = "grid") {
                            val rows = (state.posts.size + 1) / 2
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((rows * 220).dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(state.posts, key = { it.id }) { post ->
                                    PostGridItem(
                                        post = post,
                                        onDelete = { viewModel.deletePost(post.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.posts.size, key = { state.posts[it].id }) { index ->
                            PostListItem(
                                post = state.posts[index],
                                onDelete = { viewModel.deletePost(state.posts[index].id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class ViewMode {
    LIST, GRID
}

@Composable
private fun PostGridItem(
    post: com.example.article.FeedItem.Post,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (post.imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(post.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            } else {
                // Text-only post background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF42A5F5).copy(alpha = 0.1f),
                                    Color(0xFF4DD0E1).copy(alpha = 0.1f)
                                )
                            )
                        )
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp),
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = post.content,
                        fontSize = 13.sp,
                        color = if (post.imageUrl != null) Color.White else Color(0xFF1a1a1a),
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (post.imageUrl != null)
                                    Color.White.copy(alpha = 0.9f)
                                else
                                    Color(0xFF666666)
                            )
                            Text(
                                text = post.likes.toString(),
                                fontSize = 12.sp,
                                color = if (post.imageUrl != null)
                                    Color.White.copy(alpha = 0.9f)
                                else
                                    Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (post.imageUrl != null)
                                    Color.White.copy(alpha = 0.9f)
                                else
                                    Color(0xFF666666)
                            )
                            Text(
                                text = post.commentCount.toString(),
                                fontSize = 12.sp,
                                color = if (post.imageUrl != null)
                                    Color.White.copy(alpha = 0.9f)
                                else
                                    Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun PostListItem(
    post: com.example.article.FeedItem.Post,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.content,
                        fontSize = 14.sp,
                        color = Color(0xFF1a1a1a),
                        lineHeight = 20.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFD32F2F)
                    )
                }
            }

            if (post.imageUrl != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(post.imageUrl),
                        contentDescription = "Post image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Divider(
                color = Color(0xFF42A5F5).copy(alpha = 0.1f),
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF42A5F5)
                    )
                    Text(
                        text = "${post.likes} likes",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF42A5F5)
                    )
                    Text(
                        text = "${post.commentCount} comments",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}