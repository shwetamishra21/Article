package com.example.article

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx. compose. foundation. ExperimentalFoundationApi
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentScreen(
    postId: String,
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

    /* ---------- REALTIME LISTENER ---------- */
    DisposableEffect(postId) {
        val listener: ListenerRegistration =
            firestore.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        comments = snapshot.documents.mapNotNull { doc ->
                            Comment(
                                id = doc.id,
                                postId = postId,
                                authorId = doc.getString("authorId") ?: "",
                                author = doc.getString("author") ?: "User",
                                message = doc.getString("text") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
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
                title = { Text("Comments") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = allComments,
                    key = { it.id }
                ) { comment ->

                    val currentUserId = auth.currentUser?.uid

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    // ✅ AUTHOR (or admin via rules) CAN DELETE
                                    if (currentUserId == comment.authorId) {
                                        CommentRepository.deleteComment(
                                            postId = postId,
                                            commentId = comment.id,
                                            onError = { error = it }
                                        )
                                    }
                                }
                            )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = comment.author,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(comment.message)
                        }
                    }
                }
            }

            HorizontalDivider()

            /* ---------- INPUT ---------- */
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a comment…") },
                    singleLine = true
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    enabled = message.isNotBlank(),
                    onClick = {
                        val user = auth.currentUser ?: run {
                            error = "Not logged in"
                            return@Button
                        }

                        val tempId = "local_${System.currentTimeMillis()}"

                        val optimistic = Comment(
                            id = tempId,
                            postId = postId,
                            authorId = user.uid,
                            author = user.email ?: "You",
                            message = message,
                            createdAt = System.currentTimeMillis()
                        )

                        // ✅ OPTIMISTIC INSERT
                        optimisticComments = optimisticComments + optimistic
                        message = ""

                        scope.launch {
                            listState.animateScrollToItem(
                                maxOf(allComments.size, 0)
                            )
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
                    }
                ) {
                    Text("Send")
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
