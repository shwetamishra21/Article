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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.article.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class ServiceChat(
    val id: String,
    val memberName: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isActive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderInboxScreen(navController: NavController) {
    var chats by remember { mutableStateOf(listOf<ServiceChat>()) }

    LaunchedEffect(Unit) {
        // TODO: Load chats from Firebase
        chats = listOf(
            ServiceChat(
                "1", "John Doe", "When can you start the repair?",
                System.currentTimeMillis() - 1200000, 2, true
            ),
            ServiceChat(
                "2", "Jane Smith", "Thanks for the quick service!",
                System.currentTimeMillis() - 3600000, 0, false
            )
        )
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
            if (chats.isEmpty()) {
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
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chats, key = { it.id }) { chat ->
                        ChatCard(chat, navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatCard(chat: ServiceChat, navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (chat.unreadCount > 0) 6.dp else 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (chat.unreadCount > 0)
                    BluePrimary.copy(alpha = 0.4f)
                else
                    Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight,
        onClick = {
            navController.navigate("chat/${chat.id}/${chat.memberName}")
        }
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
                        text = chat.memberName.first().uppercase(),
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
                        text = chat.memberName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = formatTime(chat.timestamp),
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        fontSize = 14.sp,
                        color = if (chat.unreadCount > 0) OnSurfaceLight else Color(0xFF666666),
                        fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
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
                                    text = chat.unreadCount.toString(),
                                    color = BlueOnPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}