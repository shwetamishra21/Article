package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.InboxViewModel
import com.example.article.core.UiState
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    navController: NavController,
    onCreateRequest: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val viewModel: InboxViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId) {
        userId?.let { viewModel.loadInbox(it) }
    }

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFFE3F2FD)
        )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Inbox") }) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
        ) {

            /* ---------- TABS ---------- */
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(6.dp)
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

            Spacer(Modifier.height(16.dp))

            /* ---------- CONTENT ---------- */
            when (uiState) {
                UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Success -> {
                    val allChats =
                        (uiState as UiState.Success<List<ChatThread>>).data

                    val filteredChats = allChats.filter {
                        if (selectedTab == 0) it.type == "service"
                        else it.type == "member"
                    }

                    if (filteredChats.isEmpty()) {
                        if (selectedTab == 0) {
                            EmptyServiceInbox(onCreateRequest)
                        } else {
                            EmptyMemberInbox()
                        }
                    } else {
                        InboxList(
                            chats = filteredChats,
                            navController = navController
                        )
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = (uiState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                UiState.Idle -> Unit
            }
        }
    }
}

/* ---------- CHAT LIST ---------- */

@Composable
private fun InboxList(
    chats: List<ChatThread>,
    navController: NavController
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        chats.forEach { chat ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        navController.navigate("chat/${chat.id}/${chat.title}")
                    },
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(chat.title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        chat.lastMessage,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/* ---------- TAB ---------- */

@Composable
private fun InboxTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = if (selected) 6.dp else 0.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                if (selected) Color(0xFF42A5F5) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else Color(0xFF607D8B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White else Color(0xFF607D8B)
            )
        }
    }
}

/* ---------- EMPTY STATES ---------- */

@Composable
private fun EmptyServiceInbox(onCreateRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(52.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "No service conversations yet",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "Once a provider accepts your request,\nyou’ll be able to chat here.",
                    fontSize = 13.sp,
                    color = Color(0xFF607D8B),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onCreateRequest,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Service Request")
                }
            }
        }
    }
}

@Composable
private fun EmptyMemberInbox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No member conversations yet",
            fontSize = 14.sp,
            color = Color(0xFF607D8B)
        )
    }
}
