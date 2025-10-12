package com.example.article

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*

data class MessageItem(
    val id: Int,
    val sender: String,
    val subject: String,
    val content: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val type: MessageType = MessageType.MESSAGE
)

enum class MessagePriority { LOW, NORMAL, HIGH, URGENT }
enum class MessageType { MESSAGE, NOTIFICATION, SYSTEM, INVITE }

@Composable
fun InboxScreen() {
    var messages by remember {
        mutableStateOf(
            listOf(
                MessageItem(1, "Alice Johnson", "Project Update", "Hey! The UI design mockups are ready for review.", "10:15 AM", false, MessagePriority.HIGH, MessageType.MESSAGE),
                MessageItem(2, "System", "Welcome to Forge!", "Welcome to Forge! Start exploring amazing content.", "Yesterday", true, MessagePriority.NORMAL, MessageType.SYSTEM),
                MessageItem(3, "Bob Smith", "Collaboration Invite", "Hi! Would you like to collaborate on a project?", "2 days ago", false, MessagePriority.URGENT, MessageType.INVITE)
            )
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Unread", "Important", "System")

    val filteredMessages = when (selectedFilter) {
        "All" -> messages
        "Unread" -> messages.filter { !it.isRead }
        "Important" -> messages.filter { it.priority == MessagePriority.HIGH || it.priority == MessagePriority.URGENT }
        "System" -> messages.filter { it.type == MessageType.SYSTEM || it.type == MessageType.NOTIFICATION }
        else -> messages
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderMist)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InboxHeader(
                    unreadCount = messages.count { !it.isRead },
                    totalCount = messages.size
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Card(
                            modifier = Modifier.clickable { selectedFilter = filter },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) RoyalViolet else BrightWhite
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                filter,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                color = if (isSelected) BrightWhite else DeepPlum
                            )
                        }
                    }
                }
            }

            if (filteredMessages.isNotEmpty()) {
                items(filteredMessages, key = { it.id }) { message ->
                    MessageCard(
                        message = message,
                        onRead = { messageId ->
                            messages = messages.map {
                                if (it.id == messageId) it.copy(isRead = true)
                                else it
                            }
                        },
                        onDelete = { messageId ->
                            messages = messages.filterNot { it.id == messageId }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InboxHeader(unreadCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RoyalViolet),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "📨 Inbox",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightWhite
                )
                Text(
                    "$unreadCount unread of $totalCount messages",
                    fontSize = 14.sp,
                    color = PeachGlow
                )
            }

            if (unreadCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red),
                    shape = CircleShape
                ) {
                    Text(
                        text = unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = BrightWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MessageCard(
    message: MessageItem,
    onRead: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isExpanded = !isExpanded
                if (!message.isRead) onRead(message.id)
            },
        colors = CardDefaults.cardColors(containerColor = BrightWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RoyalViolet),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.sender.first().uppercaseChar().toString(),
                        color = BrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.sender,
                        fontWeight = FontWeight.Bold,
                        color = DeepPlum,
                        fontSize = 14.sp
                    )
                    Text(
                        text = message.subject,
                        color = SteelGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = message.timestamp,
                    color = SteelGray,
                    fontSize = 11.sp
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SoftLilac)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message.content,
                    color = SteelGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
