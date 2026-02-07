package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.article.Repository.InboxViewModel
import com.example.article.core.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    navController: NavController,
    onCreateRequest: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showMemberDialog by remember { mutableStateOf(false) }

    val viewModel: InboxViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId) {
        userId?.let { viewModel.loadInbox(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inbox",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // ✨ FAB to start new conversation (Members tab only)
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showMemberDialog = true },
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
            // ✨ PREMIUM GLOWING TABS
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

            // Content
            when (val state = uiState) {
                UiState.Loading -> {
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

                is UiState.Success<*> -> {
                    val allChats = (state.data as? List<*>)
                        ?.filterIsInstance<ChatThread>()
                        ?: emptyList()

                    val filteredChats = allChats.filter {
                        if (selectedTab == 0) it.type == "service" else it.type == "member"
                    }

                    if (filteredChats.isEmpty()) {
                        EmptyInboxState(
                            isService = selectedTab == 0,
                            onCreateRequest = onCreateRequest,
                            onStartMemberChat = { showMemberDialog = true }
                        )
                    } else {
                        InboxList(
                            chats = filteredChats,
                            navController = navController
                        )
                    }
                }

                is UiState.Error -> {
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
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                UiState.Idle -> Unit
            }
        }

        // ✨ DIALOG TO CREATE MEMBER CHAT
        if (showMemberDialog) {
            StartMemberChatDialog(
                onDismiss = { showMemberDialog = false },
                onStart = { memberName ->
                    // Create chat and navigate
                    val chatId = UUID.randomUUID().toString()
                    val firestore = FirebaseFirestore.getInstance()

                    try {
                        firestore.collection("chats").document(chatId).set(
                            mapOf(
                                "id" to chatId,
                                "title" to memberName,
                                "type" to "member",
                                "lastMessage" to "",
                                "createdAt" to System.currentTimeMillis()
                            )
                        )

                        showMemberDialog = false
                        navController.navigate("chat/$chatId/$memberName")
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            )
        }
    }
}

/* ---------- CONVERSATION LIST ---------- */

@Composable
private fun InboxList(
    chats: List<ChatThread>,
    navController: NavController
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = chats,
            key = { it.id }
        ) { chat ->
            ConversationCard(
                chat = chat,
                onClick = {
                    navController.navigate("chat/${chat.id}/${chat.title}")
                }
            )
        }
    }
}

/* ---------- PREMIUM CONVERSATION CARD ---------- */

@Composable
private fun ConversationCard(
    chat: ChatThread,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✨ Avatar with Gradient
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                    text = chat.title.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/* ---------- PREMIUM GLOWING TAB ---------- */

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

/* ---------- EMPTY STATE ---------- */

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
            // Empty Icon
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
                    "Once a provider accepts your request, you'll chat here"
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

/* ---------- DIALOG TO START MEMBER CHAT ---------- */

@Composable
private fun StartMemberChatDialog(
    onDismiss: () -> Unit,
    onStart: (String) -> Unit
) {
    var memberName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Start Conversation",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter the name of the member you want to chat with:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("Member Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (memberName.isNotBlank()) {
                        onStart(memberName.trim())
                    }
                },
                enabled = memberName.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Start Chat", fontWeight = FontWeight.SemiBold)
            }
        },
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