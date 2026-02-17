package com.example.article.chat

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedChatScreen(
    navController: NavController,
    chatId: String,
    otherUserId: String,
    otherUserName: String,
    otherUserPhoto: String = "",
    viewModel: ChatViewModel = viewModel()
) {
    // Decode URL-encoded parameters and handle 'none'
    val decodedUserName = remember {
        try {
            Uri.decode(otherUserName)
        } catch (e: Exception) {
            otherUserName
        }
    }

    val decodedUserPhoto = remember {
        try {
            val decoded = Uri.decode(otherUserPhoto)
            if (decoded == "none" || decoded.isBlank()) "" else decoded
        } catch (e: Exception) {
            if (otherUserPhoto == "none") "" else otherUserPhoto
        }
    }

    android.util.Log.d("EnhancedChatScreen", "Chat opened: $chatId")

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var lastTypingTime by remember { mutableLongStateOf(0L) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: return
    val currentUserName = currentUser.displayName ?: currentUser.email ?: "You"
    val currentUserPhoto = currentUser.photoUrl?.toString() ?: ""

    LaunchedEffect(chatId) {
        viewModel.observeMessages(
            chatId = chatId,
            currentUserId = currentUserId,
            otherUserName = decodedUserName,
            otherUserPhoto = decodedUserPhoto
        )
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.lastIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (decodedUserPhoto.isNotEmpty()) {
                            AsyncImage(
                                model = decodedUserPhoto,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = Color(0xFF42A5F5).copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = decodedUserName.firstOrNull()?.uppercase() ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF42A5F5)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = decodedUserName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            AnimatedVisibility(
                                visible = uiState.typingUsers.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = "typing...",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                        elevation = 4.dp,
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
                    )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFAFAFA))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    uiState.isLoading && uiState.messages.isEmpty() -> {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp),
                                    color = Color(0xFF42A5F5)
                                )
                            }
                        }
                    }

                    !uiState.isLoading && uiState.messages.isEmpty() -> {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFF42A5F5).copy(alpha = 0.4f)
                                    )
                                    Text(
                                        "No messages yet",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        "Say hi to $decodedUserName!",
                                        fontSize = 14.sp,
                                        color = Color(0xFF999999)
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            val isMine = message.senderId == currentUserId

                            if (message.isSystemMessage) {
                                SystemMessageBubble(message = message)
                            } else {
                                MessageBubble(
                                    message = message,
                                    isMine = isMine,
                                    showReadReceipt = isMine,
                                    isRead = message.isReadBy(otherUserId)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = Color.White.copy(alpha = 0.98f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { newText ->
                            messageText = newText

                            val now = System.currentTimeMillis()
                            if (newText.isNotBlank() && now - lastTypingTime > 1000) {
                                viewModel.onTyping(chatId, currentUserId)
                                lastTypingTime = now
                            } else if (newText.isBlank()) {
                                viewModel.onStopTyping(chatId, currentUserId)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Message...",
                                color = Color(0xFF666666).copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFF42A5F5).copy(alpha = 0.2f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                    )

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(
                                    chatId = chatId,
                                    currentUserId = currentUserId,
                                    currentUserName = currentUserName,
                                    currentUserPhoto = currentUserPhoto,
                                    text = messageText.trim(),
                                    recipientId = otherUserId
                                )
                                messageText = ""
                                viewModel.onStopTyping(chatId, currentUserId)
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (messageText.isNotBlank())
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF42A5F5),
                                            Color(0xFF4DD0E1)
                                        )
                                    )
                                else
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFE0E0E0),
                                            Color(0xFFE0E0E0)
                                        )
                                    )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank())
                                Color.White
                            else
                                Color(0xFF666666),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            android.util.Log.e("EnhancedChatScreen", "Error: $error")
            viewModel.clearError()
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    showReadReceipt: Boolean,
    isRead: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isMine) 20.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 20.dp
                ),
                color = if (isMine)
                    Color(0xFF42A5F5)
                else
                    Color.White,
                tonalElevation = if (isMine) 0.dp else 1.dp,
                shadowElevation = if (isMine) 2.dp else 1.dp
            ) {
                Box(
                    modifier = if (isMine) {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF42A5F5),
                                    Color(0xFF4DD0E1)
                                )
                            )
                        )
                    } else {
                        Modifier
                    }
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMine)
                            Color.White
                        else
                            Color(0xFF1a1a1a),
                        lineHeight = 20.sp,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMessageTime(message.timestamp.toDate().time),
                fontSize = 11.sp,
                color = Color(0xFF999999)
            )

            if (showReadReceipt) {
                Icon(
                    imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isRead) Color(0xFF42A5F5) else Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
private fun SystemMessageBubble(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE3F2FD).copy(alpha = 0.5f)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = Color(0xFF666666),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        diff < 604800_000 -> {
            val sdf = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}