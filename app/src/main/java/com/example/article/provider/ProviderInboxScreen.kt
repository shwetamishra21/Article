package com.example.article.provider

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.article.chat.ChatHelper
import com.example.article.chat.ChatRepository
import com.example.article.chat.ChatThread
import com.example.article.chat.InboxViewModel
import com.example.article.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderInboxScreen(
    navController: NavController,
    viewModel: InboxViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    var chatToDelete by remember { mutableStateOf<ChatThread?>(null) }

    LaunchedEffect(userId) {
        userId?.let {
            viewModel.loadInbox(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Service Chats",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BlueOnPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BluePrimary, BlueSecondary)
                        )
                    )
                    .shadow(
                        elevation = 6.dp,
                        spotColor = BluePrimary.copy(alpha = 0.4f)
                    )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp),
                            color = BluePrimary
                        )
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Unable to load chats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                            Button(
                                onClick = { userId?.let { viewModel.loadInbox(it) } },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BluePrimary
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState.chats.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF999999).copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No active chats",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Accept service requests to start chatting",
                            fontSize = 14.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.chats, key = { it.id }) { chat ->
                            ServiceChatCard(
                                chat = chat,
                                currentUserId = userId ?: "",
                                onClick = {
                                    scope.launch {
                                        // Mark as read before navigating
                                        ChatRepository.markChatAsRead(chat.id, userId ?: "")

                                        // Navigate to chat
                                        ChatHelper.navigateToChat(
                                            navController = navController,
                                            chat = chat,
                                            currentUserId = userId ?: ""
                                        )
                                    }
                                },
                                onDelete = { chatToDelete = chat }
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (chatToDelete != null) {
            AlertDialog(
                onDismissRequest = { chatToDelete = null },
                title = { Text("Delete Conversation") },
                text = {
                    Text("Are you sure you want to delete this conversation with ${
                        chatToDelete?.getOtherUserName(userId ?: "")
                    }?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                ChatRepository.deleteChat(chatToDelete!!.id)
                                chatToDelete = null
                            }
                        }
                    ) {
                        Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chatToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceChatCard(
    chat: ChatThread,
    currentUserId: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val unreadCount = chat.getUnreadCount(currentUserId)
    val isTyping = chat.isOtherUserTyping(currentUserId)
    val otherUserName = chat.getOtherUserName(currentUserId)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (unreadCount > 0) 6.dp else 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (unreadCount > 0)
                    BluePrimary.copy(alpha = 0.4f)
                else
                    Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (unreadCount > 0) {
            BluePrimary.copy(alpha = 0.08f)
        } else {
            SurfaceLight
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = BluePrimary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Chat Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherUserName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = formatTime(chat.lastMessageAt),
                        fontSize = 12.sp,
                        color = if (unreadCount > 0) BluePrimary else Color(0xFF999999),
                        fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Service request info (if available)
                if (chat.serviceRequestId != null) {
                    Text(
                        text = "Service Request",
                        fontSize = 11.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show typing or last message
                    if (isTyping) {
                        Text(
                            text = "typing...",
                            fontSize = 14.sp,
                            color = BluePrimary,
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = chat.lastMessage.ifEmpty { "No messages yet" },
                            fontSize = 14.sp,
                            color = if (unreadCount > 0) OnSurfaceLight else Color(0xFF666666),
                            fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = BluePrimary
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = BlueOnPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color(0xFF666666)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            onDelete()
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

private fun formatTime(timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val timestampMillis = timestamp.toDate().time
    val diff = now - timestampMillis

    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> {
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            sdf.format(timestamp.toDate())
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(timestamp.toDate())
        }
    }
}