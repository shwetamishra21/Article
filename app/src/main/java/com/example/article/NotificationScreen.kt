package com.example.article.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com. example. article. Repository. AppNotification
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import com. example. article. Repository. NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startObservingFromSession()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllAsRead() }
                        ) {
                            Text(
                                "Mark all read",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF42A5F5), Color(0xFF4DD0E1))
                        )
                    )
                    .shadow(6.dp, spotColor = Color(0xFF42A5F5).copy(alpha = 0.4f))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color(0xFF42A5F5),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                uiState.notifications.isEmpty() -> {
                    EmptyNotificationsState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Unread section header
                        val unread = uiState.notifications.filter { !it.read }
                        val read = uiState.notifications.filter { it.read }

                        if (unread.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "New",
                                    count = unread.size
                                )
                            }
                            items(unread, key = { it.id }) { notif ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    NotificationCard(
                                        notification = notif,
                                        onTap = {
                                            viewModel.markAsRead(notif.id)
                                            handleNotificationTap(notif, navController)
                                        },
                                        onDismiss = { viewModel.delete(notif.id) }
                                    )
                                }
                            }
                        }

                        if (read.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Earlier")
                            }
                            items(read, key = { it.id }) { notif ->
                                NotificationCard(
                                    notification = notif,
                                    onTap = { handleNotificationTap(notif, navController) },
                                    onDismiss = { viewModel.delete(notif.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleNotificationTap(notif: AppNotification, navController: NavController) {
    when (notif.type) {
        AppNotification.TYPE_MESSAGE -> {
            if (notif.referenceId.isNotBlank()) {
                // referenceId = "chatId|otherUserId|otherUserName"
                val parts = notif.referenceId.split("|")
                if (parts.size >= 3) {
                    val chatId = parts[0]
                    val otherUserId = parts[1]
                    val otherUserName = parts[2]
                    navController.navigate("chat/$chatId/$otherUserId/$otherUserName/none")
                }
            }
        }
        AppNotification.TYPE_SERVICE_REQUEST -> {
            navController.navigate("requests")
        }
        AppNotification.TYPE_ANNOUNCEMENT -> {
            navController.navigate("home")
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF888888),
            letterSpacing = 0.5.sp
        )
        if (count != null) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF42A5F5)
            ) {
                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onTap: () -> Unit,
    onDismiss: () -> Unit
) {
    val isUnread = !notification.read
    val (icon, iconBg, iconTint) = notificationStyle(notification.type)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isUnread) 4.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isUnread)
                    Color(0xFF42A5F5).copy(alpha = 0.2f)
                else
                    Color.Black.copy(alpha = 0.05f)
            )
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        color = if (isUnread) Color.White else Color(0xFFFAFAFA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isUnread) Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF42A5F5).copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    ) else Modifier
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon bubble
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = iconBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 14.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isUnread) Color(0xFF0D1B2A) else Color(0xFF444444),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatNotifTime(notification.createdAt.toDate().time),
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }

                Text(
                    text = notification.body,
                    fontSize = 13.sp,
                    color = if (isUnread) Color(0xFF333333) else Color(0xFF888888),
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Type chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iconBg.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = notificationTypeLabel(notification.type),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            // Unread dot + dismiss button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF42A5F5))
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF42A5F5).copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF42A5F5).copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                "All caught up!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1a1a1a)
            )
            Text(
                "You'll be notified about announcements,\nmessages, and service request updates.",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 21.sp
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private data class NotifStyle(
    val icon: ImageVector,
    val bg: Color,
    val tint: Color
)

@Composable
private fun notificationStyle(type: String): NotifStyle = when (type) {
    AppNotification.TYPE_ANNOUNCEMENT -> NotifStyle(
        icon = Icons.Default.Campaign,
        bg = Color(0xFFE3F2FD),
        tint = Color(0xFF1976D2)
    )
    AppNotification.TYPE_MESSAGE -> NotifStyle(
        icon = Icons.Default.Chat,
        bg = Color(0xFFE8F5E9),
        tint = Color(0xFF388E3C)
    )
    AppNotification.TYPE_SERVICE_REQUEST -> NotifStyle(
        icon = Icons.Default.Build,
        bg = Color(0xFFFFF3E0),
        tint = Color(0xFFE65100)
    )
    else -> NotifStyle(
        icon = Icons.Default.Notifications,
        bg = Color(0xFFF3E5F5),
        tint = Color(0xFF7B1FA2)
    )
}

private fun notificationTypeLabel(type: String): String = when (type) {
    AppNotification.TYPE_ANNOUNCEMENT -> "ANNOUNCEMENT"
    AppNotification.TYPE_MESSAGE -> "MESSAGE"
    AppNotification.TYPE_SERVICE_REQUEST -> "SERVICE REQUEST"
    else -> "NOTIFICATION"
}

private fun formatNotifTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}