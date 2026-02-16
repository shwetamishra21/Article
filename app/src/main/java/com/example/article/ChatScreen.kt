package com.example.article

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.article.Repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String,
    title: String,
    viewModel: ChatViewModel = viewModel()
) {
    var message by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var selectedMessageForDelete by remember { mutableStateOf<ChatMessage?>(null) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

    // Observe messages AND mark as read when opening
    LaunchedEffect(chatId) {
        viewModel.observeMessages(chatId)
        ChatRepository.markChatAsRead(chatId, currentUserId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // Show error snackbar if needed
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (uiState.isLoading) {
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // ✅ Delete Chat Button
                    IconButton(onClick = { showDeleteChatDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete chat",
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFAFAFA))
        ) {
            // Messages List with Date Separators
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF42A5F5).copy(alpha = 0.1f),
                                                        Color(0xFF42A5F5).copy(alpha = 0.05f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = Color(0xFF42A5F5).copy(alpha = 0.6f)
                                        )
                                    }

                                    Text(
                                        text = "No messages yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Start the conversation!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF666666),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // ✅ Group messages by date
                        val groupedMessages = uiState.messages.groupBy { msg ->
                            getDateLabel(msg.timestamp)
                        }

                        groupedMessages.forEach { (dateLabel, messagesInDate) ->
                            // ✅ Date Separator
                            item(key = "date_$dateLabel") {
                                DateSeparator(dateLabel)
                            }

                            // Messages for this date
                            items(
                                items = messagesInDate,
                                key = { it.id }
                            ) { msg ->
                                MessageBubble(
                                    message = msg,
                                    isMine = msg.senderId == currentUserId,
                                    onLongPress = {
                                        selectedMessageForDelete = msg
                                    },
                                    onCopy = {
                                        copyToClipboard(context, msg.text)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied to clipboard")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Input Area
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
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Type a message…",
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
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                        enabled = !uiState.isSending
                    )

                    IconButton(
                        onClick = {
                            if (message.isNotBlank() && !uiState.isSending) {
                                viewModel.sendMessage(
                                    chatId = chatId,
                                    text = message.trim(),
                                    senderId = currentUserId
                                )
                                message = ""
                            }
                        },
                        enabled = message.isNotBlank() && !uiState.isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (message.isNotBlank() && !uiState.isSending)
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
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (message.isNotBlank())
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
    }

    // ✅ Delete Message Dialog
    if (selectedMessageForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedMessageForDelete = null },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to delete this message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(
                            chatId = chatId,
                            messageId = selectedMessageForDelete!!.id,
                            userId = currentUserId
                        )
                        selectedMessageForDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMessageForDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ✅ Delete Chat Dialog
    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this entire conversation? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            ChatRepository.deleteChat(chatId, currentUserId)
                            showDeleteChatDialog = false
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ✅ Date Separator Component
@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE3F2FD)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
        }
    }
}

// ✅ Enhanced Message Bubble with Long Press Actions
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    onLongPress: () -> Unit,
    onCopy: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { showMenu = true }
                    ),
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
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMine)
                                Color.White
                            else
                                Color(0xFF1a1a1a),
                            lineHeight = 20.sp,
                            fontSize = 15.sp
                        )

                        // ✅ Timestamp
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatTime(message.timestamp),
                                fontSize = 11.sp,
                                color = if (isMine)
                                    Color.White.copy(alpha = 0.7f)
                                else
                                    Color(0xFF666666)
                            )

                            // ✅ Delivery Status (for sent messages)
                            if (isMine) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (message.read) Icons.Default.Done else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (message.read)
                                        Color(0xFF4CAF50)
                                    else
                                        Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ✅ Context Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        onCopy()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    }
                )

                if (isMine) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            onLongPress()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }
    }
}

// ✅ Utility Functions

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getDateLabel(timestamp: Long): String {
    val messageDate = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }

    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }

    return when {
        isSameDay(messageDate, today) -> "Today"
        isSameDay(messageDate, yesterday) -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("message", text)
    clipboard.setPrimaryClip(clip)
}