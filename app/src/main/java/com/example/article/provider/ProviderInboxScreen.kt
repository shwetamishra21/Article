package com.example.article.provider

import androidx.compose.foundation.background
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
import com.example.article.SimpleUser
import com.example.article.chat.ChatHelper
import com.example.article.chat.ChatRepository
import com.example.article.chat.ChatThread
import com.example.article.chat.InboxViewModel
import com.example.article.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    var showUserPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        userId?.let { viewModel.loadInbox(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
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
                        Brush.linearGradient(colors = listOf(BluePrimary, BlueSecondary))
                    )
                    .shadow(elevation = 6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUserPickerDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = BluePrimary
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = "Message a member",
                    tint = BlueOnPrimary
                )
            }
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp),
                            color = BluePrimary
                        )
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Unable to load chats", fontWeight = FontWeight.SemiBold)
                            Text(
                                uiState.error ?: "Unknown error",
                                color = Color(0xFF666666),
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { userId?.let { viewModel.loadInbox(it) } },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) { Text("Retry") }
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
                        Text("No messages yet", fontSize = 16.sp, color = Color(0xFF666666))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap + to message a member",
                            fontSize = 14.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.chats, key = { it.id }) { chat ->
                            ServiceChatCard(
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

        // User picker dialog — shows only members and admins (not other providers)
        if (showUserPickerDialog) {
            ProviderUserPickerDialog(
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
                }
            )
        }

        // Delete Confirmation Dialog
        if (chatToDelete != null) {
            AlertDialog(
                onDismissRequest = { chatToDelete = null },
                title = { Text("Delete Conversation", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Delete conversation with ${chatToDelete?.getOtherUserName(userId ?: "")}?")
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
    val otherUserRole = chat.getOtherUserRole(currentUserId)

    val roleLabel = when (otherUserRole) {
        "admin" -> "Admin"
        "service_provider" -> "Provider"
        else -> "Member"
    }
    val roleColor = when (otherUserRole) {
        "admin" -> Color(0xFFFF9800)
        else -> BluePrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (unreadCount > 0) 6.dp else 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (unreadCount > 0) BluePrimary.copy(alpha = 0.4f)
                else Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (unreadCount > 0) BluePrimary.copy(alpha = 0.08f) else SurfaceLight,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with role dot
            Box(modifier = Modifier.size(50.dp)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = roleColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                            color = roleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                // Role dot
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
                            fontSize = 15.sp,
                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = OnSurfaceLight,
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
                        text = formatTime(chat.lastMessageAt),
                        fontSize = 12.sp,
                        color = if (unreadCount > 0) BluePrimary else Color(0xFF999999),
                        fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                if (chat.serviceRequestId != null) {
                    Text(
                        text = "Service Request",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
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
                            fontSize = 14.sp,
                            color = BluePrimary,
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (unreadCount > 0) {
                            Surface(shape = CircleShape, color = BluePrimary) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = BlueOnPrimary,
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
                                    tint = Color(0xFF666666),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderUserPickerDialog(
    onDismiss: () -> Unit,
    onUserSelected: (SimpleUser) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<SimpleUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        isLoading = true
        users = loadMembersAndAdmins(currentUserId)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message a Member", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search members...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = BluePrimary) }
                } else {
                    val filtered = if (searchQuery.isBlank()) users
                    else users.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (searchQuery.isBlank()) "No members found" else "No matching members",
                                color = Color(0xFF666666)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            items(filtered, key = { it.uid }) { user ->
                                ProviderUserPickerItem(
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderUserPickerItem(user: SimpleUser, onClick: () -> Unit) {
    val roleColor = if (user.role == "admin") Color(0xFFFF9800) else BluePrimary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = BluePrimary.copy(alpha = 0.06f)
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
                    text = if (user.role == "admin") "Admin" else "Member",
                    fontSize = 12.sp,
                    color = roleColor
                )
            }
        }
    }
}

private suspend fun loadMembersAndAdmins(currentUserId: String?): List<SimpleUser> {
    return try {
        FirebaseFirestore.getInstance().collection("users").get().await()
            .documents.mapNotNull { doc ->
                if (doc.id == currentUserId) return@mapNotNull null
                val role = doc.getString("role") ?: "member"
                // Providers can message members and admins only
                if (role == "service_provider") return@mapNotNull null
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

private fun formatTime(timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp.toDate().time
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp.toDate())
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())
    }
}