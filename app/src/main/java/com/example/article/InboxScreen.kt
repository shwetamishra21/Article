package com.example.article

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
import androidx.compose.ui.draw.scale
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
import com.example.article.chat.InboxUiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class SimpleUser(
    val uid: String,
    val name: String,
    val photoUrl: String = "",
    val role: String = "member"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    navController: NavController,
    onCreateRequest: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showUserPickerDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<ChatThread?>(null) }

    val viewModel: InboxViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val userId = FirebaseAuth.getInstance().currentUser?.uid

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
                        text = "Messages",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUserPickerDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Start conversation",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
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
                )
        ) {
            // Tab Selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InboxTab(
                        icon = Icons.Default.Build,
                        label = "Services",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    InboxTab(
                        icon = Icons.Default.Group,
                        label = "Members",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val currentState = uiState

            when {
                currentState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                currentState.error != null -> {
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
                                text = "Unable to load inbox",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentState.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { userId?.let { viewModel.loadInbox(it) } },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    val allChats = currentState.chats

                    // Filter chats based on selected tab and participant roles
                    val filteredChats = allChats.filter { chat ->
                        val otherUserId = chat.getOtherUserId(userId ?: "") ?: ""
                        val otherUserRole = chat.getOtherUserRole(userId ?: "")

                        when (selectedTab) {
                            // Services tab - show service_provider conversations
                            0 -> chat.type == "service" || otherUserRole == "service_provider"
                            // Members tab - show member conversations (exclude service providers)
                            1 -> chat.type == "member" && otherUserRole != "service_provider"
                            else -> false
                        }
                    }

                    if (filteredChats.isEmpty()) {
                        EmptyInboxState(
                            isService = selectedTab == 0,
                            onCreateRequest = onCreateRequest,
                            onStartMemberChat = { showUserPickerDialog = true }
                        )
                    } else {
                        InboxList(
                            chats = filteredChats,
                            navController = navController,
                            currentUserId = userId ?: "",
                            onDeleteChat = { chatToDelete = it }
                        )
                    }
                }
            }
        }

        // User Picker Dialog
        if (showUserPickerDialog) {
            UserPickerDialog(
                onDismiss = { showUserPickerDialog = false },
                onUserSelected = { selectedUser ->
                    showUserPickerDialog = false
                    scope.launch {
                        ChatHelper.startChatWith(
                            navController = navController,
                            scope = scope,
                            otherUserId = selectedUser.uid,
                            otherUserName = selectedUser.name,
                            otherUserPhoto = selectedUser.photoUrl,
                            otherUserRole = selectedUser.role
                        )
                    }
                },
                filterServiceProviders = selectedTab == 1 // Filter out providers in Members tab
            )
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
                }
            )
        }
    }
}

@Composable
private fun InboxList(
    chats: List<ChatThread>,
    navController: NavController,
    currentUserId: String,
    onDeleteChat: (ChatThread) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = chats,
            key = { it.id }
        ) { chat ->
            ConversationCard(
                chat = chat,
                currentUserId = currentUserId,
                onClick = {
                    scope.launch {
                        // Mark as read before navigating
                        ChatRepository.markChatAsRead(chat.id, currentUserId)

                        // Navigate to chat
                        ChatHelper.navigateToChat(
                            navController = navController,
                            chat = chat,
                            currentUserId = currentUserId
                        )
                    }
                },
                onDelete = { onDeleteChat(chat) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationCard(
    chat: ChatThread,
    currentUserId: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val unreadCount = chat.getUnreadCount(currentUserId)
    val isTyping = chat.isOtherUserTyping(currentUserId)
    val otherUserName = chat.getOtherUserName(currentUserId)
    val otherUserRole = chat.getOtherUserRole(currentUserId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (unreadCount > 0) 3.dp else 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (unreadCount > 0) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Name + Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = otherUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Role badge
                        if (otherUserRole == "service_provider") {
                            Text(
                                text = "Service Provider",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Time ago
                    Text(
                        text = formatTimeAgo(chat.lastMessageAt),
                        fontSize = 13.sp,
                        color = if (unreadCount > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Last Message + Unread Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show typing indicator or last message
                    if (isTyping) {
                        Text(
                            text = "typing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = chat.lastMessage.ifEmpty { "No messages yet" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (unreadCount > 0)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Unread Badge
                    if (unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3-dot Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

// Format time ago (like WhatsApp) - Updated to handle Timestamp
private fun formatTimeAgo(timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val timestampMillis = timestamp.toDate().time
    val diff = now - timestampMillis

    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 172800_000 -> "Yesterday"
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

@Composable
private fun InboxTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.then(
            if (selected) Modifier.scale(1.02f) else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.01f)
        },
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Box(
            modifier = if (selected) {
                Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
            } else {
                Modifier
            }
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyInboxState(
    isService: Boolean,
    onCreateRequest: () -> Unit,
    onStartMemberChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isService) Icons.Default.Build else Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            Text(
                text = if (isService)
                    "No service conversations yet"
                else
                    "No member conversations yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )

            Text(
                text = if (isService)
                    "Connect with service providers for your needs"
                else
                    "Start connecting with your neighbors!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = if (isService) onCreateRequest else onStartMemberChat,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isService) "Create Service Request" else "Start Conversation",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun UserPickerDialog(
    onDismiss: () -> Unit,
    onUserSelected: (SimpleUser) -> Unit,
    filterServiceProviders: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<SimpleUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        isLoading = true
        users = loadAllUsers(currentUserId, filterServiceProviders)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Start Conversation",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search users...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredUsers = if (searchQuery.isBlank()) {
                        users
                    } else {
                        users.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No users found" else "No matching users",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(filteredUsers) { user ->
                                UserPickerItem(
                                    user = user,
                                    onClick = { onUserSelected(user) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun UserPickerItem(
    user: SimpleUser,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (user.role) {
                        "service_provider" -> "Service Provider"
                        "admin" -> "Admin"
                        else -> "Member"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun loadAllUsers(
    currentUserId: String?,
    filterServiceProviders: Boolean
): List<SimpleUser> {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val snapshot = firestore.collection("users")
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            if (doc.id == currentUserId) return@mapNotNull null

            val role = doc.getString("role") ?: "member"

            // Filter out service providers if in Members tab
            if (filterServiceProviders && role == "service_provider") {
                return@mapNotNull null
            }

            SimpleUser(
                uid = doc.id,
                name = doc.getString("name") ?: "Unknown User",
                photoUrl = doc.getString("photoUrl") ?: "",
                role = role
            )
        }.sortedBy { it.name }

    } catch (e: Exception) {
        emptyList()
    }
}