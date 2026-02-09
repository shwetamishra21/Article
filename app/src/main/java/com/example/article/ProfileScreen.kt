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
import androidx.compose.foundation.lazy.items
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

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadProfileImage(it) {
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Error loading profile",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            state.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadProfile() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
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
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        ),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item(key = "header") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Profile Image
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .border(
                                            width = 3.dp,
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .clickable { imagePicker.launch("image/*") }
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isUpdating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }

                                // Name & Bio
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Name", fontSize = 13.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = editBio,
                                        onValueChange = { editBio = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Bio", fontSize = 13.sp) },
                                        maxLines = 3,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Text(
                                        text = profile.name.ifEmpty { "Set your name" },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = profile.bio.ifEmpty { "Add a bio" },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }

                                // Role Badge
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = role.name.lowercase().replace("_", " ")
                                            .replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Edit/Save Buttons
                                if (isEditing) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { isEditing = false },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isUpdating
                                        ) {
                                            Text("Cancel", fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.updateProfile(editName, editBio) {
                                                    isEditing = false
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isUpdating
                                        ) {
                                            if (isUpdating) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                            } else {
                                                Text("Save", fontSize = 13.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            editName = profile.name
                                            editBio = profile.bio
                                            isEditing = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Edit Profile", fontSize = 13.sp)
                                    }
                                }

                                if (role != UserRole.SERVICE_PROVIDER) {
                                    OutlinedButton(
                                        onClick = onCreatePost,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Create New Post", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "posts_header") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = "My Posts (${state.posts.size})",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (state.posts.isEmpty()) {
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
                                    Icon(
                                        Icons.Default.Article,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                    Text(
                                        "No posts yet",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.posts, key = { it.id }) { post ->
                            PostListItem(
                                post = post,
                                onDelete = { viewModel.deletePost(post.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostListItem(
    post: com.example.article.FeedItem.Post,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.content,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        maxLines = 3
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${post.likes} likes · ${post.commentCount} comments",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}