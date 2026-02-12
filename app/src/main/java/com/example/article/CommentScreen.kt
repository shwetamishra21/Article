package com.example.article

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommentScreen(
    postId: String,
    postAuthorId: String,
    onBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var optimisticComments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val currentUserId = auth.currentUser?.uid ?: ""

    // Ensure we have a valid post before proceeding
    LaunchedEffect(postId) {
        if (postId.isBlank()) {
            error = "Invalid post"
            loading = false
        }
    }

    /* ---------- REALTIME LISTENER ---------- */
    DisposableEffect(postId) {
        val listener: ListenerRegistration =
            firestore.collection("posts")
                .document(postId)
                .collection("comments")
                .addSnapshotListener { snapshot, firebaseError ->
                    if (firebaseError != null) {
                        error = "Failed to load comments: ${firebaseError.localizedMessage}"
                        loading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        comments = snapshot.documents.mapNotNull { doc ->
                            try {
                                Comment(
                                    id = doc.id,
                                    postId = postId,
                                    authorId = doc.getString("authorId") ?: "",
                                    author = doc.getString("author") ?: "User",
                                    message = doc.getString("text") ?: "",
                                    createdAt = doc.getLong("createdAt") ?: 0L
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedBy { it.createdAt }
                        optimisticComments = emptyList()
                        loading = false
                    }
                }

        onDispose { listener.remove() }
    }

    val allComments = remember(comments, optimisticComments) {
        (comments + optimisticComments).distinctBy { it.id }
    }

    /* ---------------- UI ---------------- */

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (loading && allComments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (allComments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No comments yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Be the first to share your thoughts!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(
                        items = allComments,
                        key = { it.id }
                    ) { comment ->
                        CommentCard(
                            comment = comment,
                            currentUserId = currentUserId,
                            postOwnerId = postAuthorId,
                            onDelete = {
                                CommentRepository.deleteComment(postId, comment.id, onError = { error = it })
                            }
                        )

                    }
                }
            }

            // Input Area
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = "Write a comment…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = false,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Send Button
                        IconButton(
                            onClick = {
                                val user = auth.currentUser ?: run {
                                    error = "Not logged in"
                                    return@IconButton
                                }

                                if (message.isBlank()) return@IconButton

                                val tempId = "local_${System.currentTimeMillis()}"

                                val optimistic = Comment(
                                    id = tempId,
                                    postId = postId,
                                    authorId = user.uid,
                                    author = user.email ?: "You",
                                    message = message.trim(),
                                    createdAt = System.currentTimeMillis()
                                )

                                optimisticComments = optimisticComments + optimistic
                                message = ""

                                scope.launch {
                                    try {
                                        listState.animateScrollToItem(
                                            index = allComments.size
                                        )
                                    } catch (e: Exception) {
                                        // Ignore scroll errors
                                    }
                                }

                                CommentRepository.addComment(
                                    postId = postId,
                                    authorId = user.uid,
                                    author = user.email ?: "User",
                                    text = optimistic.message,
                                    onComplete = { error = null },
                                    onError = { err ->
                                        optimisticComments =
                                            optimisticComments.filterNot { it.id == tempId }
                                        error = err
                                    }
                                )
                            },
                            enabled = message.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (message.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (message.isNotBlank())
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Error Message
                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentCard(
    comment: Comment,
    currentUserId: String,
    postOwnerId: String,
    onDelete: () -> Unit
) {
    val canDelete = currentUserId == comment.authorId || currentUserId == postOwnerId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canDelete) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onDelete
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.author.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        comment.author,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentUserId == comment.authorId) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "You",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Text(
                    comment.message,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
                Text(
                    "Just now",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}