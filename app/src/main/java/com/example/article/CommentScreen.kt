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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

// Safe Timestamp helper
private fun DocumentSnapshot.getTimestampSafe(field: String): Timestamp {
    return try {
        getTimestamp(field) ?: Timestamp.now()
    } catch (e: RuntimeException) {
        Log.w("CommentScreen", "Invalid $field, using now")
        Timestamp.now()
    }
}

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

    // Get current user name and photo
    var currentUserName by remember { mutableStateOf(auth.currentUser?.email ?: "You") }
    var currentUserPhoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUserName = document.getString("name") ?: currentUserName
                        currentUserPhoto = document.getString("profilePictureUrl")
                    }
                }
        }
    }

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
                .orderBy("createdAt")
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
                                    id = doc.getString("id") ?: doc.id,
                                    postId = doc.getString("postId") ?: postId,
                                    authorId = doc.getString("authorId") ?: "",
                                    authorName = doc.getString("authorName") ?: "User",
                                    authorPhotoUrl = doc.getString("authorPhotoUrl") ?: "",
                                    content = doc.getString("content") ?: "",
                                    createdAt = doc.getTimestampSafe("createdAt")
                                )
                            } catch (e: Exception) {
                                Log.e("CommentScreen", "Failed to parse comment ${doc.id}", e)
                                null
                            }
                        }
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
                        fontWeight = FontWeight.SemiBold
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
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allComments, key = { it.id }) { comment ->
                        CommentCard(
                            comment = comment,
                            currentUserId = currentUserId,
                            postOwnerId = postAuthorId,
                            onDelete = {
                                CommentRepository.deleteComment(
                                    postId,
                                    comment.id,
                                    onError = { error = it }
                                )
                            }
                        )
                    }
                }
            }

            /* -------- INPUT AREA -------- */

            Surface(tonalElevation = 3.dp) {
                Column {
                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Write a comment…") },
                            shape = RoundedCornerShape(24.dp)
                        )

                        IconButton(
                            onClick = {
                                val user = auth.currentUser ?: return@IconButton
                                if (message.isBlank()) return@IconButton

                                val tempId = "local_${System.currentTimeMillis()}"

                                val optimistic = Comment(
                                    id = tempId,
                                    postId = postId,
                                    authorId = user.uid,
                                    authorName = currentUserName,
                                    authorPhotoUrl = currentUserPhoto ?: "",
                                    content = message.trim(),
                                    createdAt = Timestamp.now()
                                )

                                optimisticComments = optimisticComments + optimistic
                                message = ""

                                scope.launch {
                                    listState.animateScrollToItem(allComments.size)
                                }

                                CommentRepository.addComment(
                                    postId = postId,
                                    authorId = user.uid,
                                    authorName = currentUserName,
                                    authorPhotoUrl = currentUserPhoto ?: "",
                                    content = optimistic.content,
                                    onComplete = { error = null },
                                    onError = { err ->
                                        optimisticComments =
                                            optimisticComments.filterNot { it.id == tempId }
                                        error = err
                                    }
                                )
                            },
                            enabled = message.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send"
                            )
                        }
                    }

                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
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
    val isDeleting = false
    val canDelete = currentUserId == comment.authorId || currentUserId == postOwnerId

    // Fetch user profile data if not already present
    var userName by remember { mutableStateOf(comment.authorName) }
    var userPhotoUrl by remember { mutableStateOf(comment.authorPhotoUrl) }

    LaunchedEffect(comment.authorId) {
        if (userName.contains("@") || userName == "User") {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users")
                .document(comment.authorId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("name") ?: comment.authorName
                        userPhotoUrl = document.getString("profilePictureUrl") ?: comment.authorPhotoUrl
                    }
                }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDeleting) 0.5f else 1f)
            .combinedClickable(
                enabled = canDelete,
                onClick = {},
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp)) {

            // Profile picture or avatar
            if (!userPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userPhotoUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(userName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    comment.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatCommentTimestamp(comment.createdAt.toDate().time),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Timestamp formatter for comments
private fun formatCommentTimestamp(timestamp: Long): String {
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