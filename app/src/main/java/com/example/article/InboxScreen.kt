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
        userId?.let { viewModel.loadInbox(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) onCreateRequest()
                    else showUserPickerDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (selectedTab == 0) Icons.Default.Add else Icons.Default.PersonAdd,
                    contentDescription = null,
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
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
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

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
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
                            Text("Unable to load inbox", fontWeight = FontWeight.SemiBold)
                            Text(
                                uiState.error ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { userId?.let { viewModel.loadInbox(it) } },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Retry") }
                        }
                    }
                }

                else -> {
                    val filteredChats = uiState.chats.filter { chat ->
                        val otherUserRole = chat.getOtherUserRole(userId ?: "")
                        when (selectedTab) {
                            0 -> chat.type == "service" || otherUserRole == "service_provider"
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
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 8.dp, bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredChats, key = { it.id }) { chat ->
                                ConversationCard(
                                    chat = chat,
                                    currentUserId = userId ?: "",
                                    onClick = {
                                        scope.launch {
                                            ChatRepository.markChatAsRead(chat.id, userId ?: "")
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
        }

        if (showUserPickerDialog) {
            UserPickerDialog(
                onDismiss = { showUserPickerDialog = false },
                onUserSelected = { user ->
                    showUserPickerDialog = false
                    ChatHelper.startChatWith(
                        navController = navController,
                        scope = scope,
                        otherUserId = user.uid,
                        otherUserName = user.name,
                        otherUserPhoto = user.photoUrl,
                        otherUserRole = user.role
                    )
                },
                filterServiceProviders = selectedTab == 1
            )
        }

        if (chatToDelete != null) {
            AlertDialog(
                onDismissRequest = { chatToDelete = null },
                title = { Text("Delete Conversation", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Delete your conversation with ${chatToDelete?.getOtherUserName(userId ?: "")}?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            ChatRepository.deleteChat(chatToDelete!!.id)
                            chatToDelete = null
                        }
                    }) {
                        Text("Delete", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chatToDelete = null }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(16.dp)
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

    val roleColor = when (otherUserRole) {
        "service_provider" -> Color(0xFF4CAF50)
        "admin" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }
    val roleLabel = when (otherUserRole) {
        "service_provider" -> "Provider"
        "admin" -> "Admin"
        else -> "Member"
    }

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
            containerColor = if (unreadCount > 0)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with role dot
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(roleColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                        color = roleColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(roleColor)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = otherUserName,
                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = roleColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = roleLabel,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = roleColor
                            )
                        }
                    }
                    Text(
                        text = formatTimeAgo(chat.lastMessageAt),
                        fontSize = 11.sp,
                        color = if (unreadCount > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTyping) {
                        Text(
                            text = "typing...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = chat.lastMessage.ifEmpty { "No messages yet" },
                            fontSize = 13.sp,
                            color = if (unreadCount > 0)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (unreadCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color(0xFFB71C1C)) },
                                    onClick = { onDelete(); showMenu = false },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFB71C1C))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp.toDate().time
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate())
        diff < 172800_000 -> "Yesterday"
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp.toDate())
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        modifier = Modifier.fillMaxSize().padding(32.dp),
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
                            listOf(
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
                text = if (isService) "No service conversations yet" else "No member conversations yet",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Text(
                text = if (isService) "Create a service request to connect with providers"
                else "Start chatting with your neighbors!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
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
        title = { Text("Start Conversation", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search users...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    val filtered = if (searchQuery.isBlank()) users
                    else users.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (searchQuery.isBlank()) "No users found" else "No matching users",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            items(filtered, key = { it.uid }) { user ->
                                UserPickerItem(user = user, onClick = { onUserSelected(user) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserPickerItem(user: SimpleUser, onClick: () -> Unit) {
    val roleColor = when (user.role) {
        "service_provider" -> Color(0xFF4CAF50)
        "admin" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    color = roleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    text = when (user.role) {
                        "service_provider" -> "Service Provider"
                        "admin" -> "Admin"
                        else -> "Member"
                    },
                    fontSize = 12.sp,
                    color = roleColor
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
        FirebaseFirestore.getInstance().collection("users").get().await()
            .documents.mapNotNull { doc ->
                if (doc.id == currentUserId) return@mapNotNull null
                val role = doc.getString("role") ?: "member"
                if (filterServiceProviders && role == "service_provider") return@mapNotNull null
                SimpleUser(
                    uid = doc.id,
                    name = doc.getString("name") ?: "Unknown",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    role = role
                )
            }.sortedBy { it.name }
    } catch (e: Exception) {
        emptyList()
    }
}