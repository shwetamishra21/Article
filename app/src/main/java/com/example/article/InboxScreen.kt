package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .background(MaterialTheme.colorScheme.background)
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
                        ForgeCard(
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                filter,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
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
                                if (it.id == messageId) it.copy(isRead = true) else it
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
    ForgeCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "📨 Inbox",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "$unreadCount unread of $totalCount messages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (unreadCount > 0) {
                ForgeCard(
                    modifier = Modifier.size(28.dp),
                    content = {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = unreadCount.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                )
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

    ForgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isExpanded = !isExpanded
                if (!message.isRead) onRead(message.id)
            }
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
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.sender.first().uppercaseChar().toString(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = message.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
