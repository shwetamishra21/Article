package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.article.ui.theme.*

data class MessageItem(
    val sender: String,
    val content: String,
    val timestamp: String
)

@Composable
fun InboxScreen() {
    val messages = listOf(
        MessageItem("Alice", "Hey, how are you?", "10:15 AM"),
        MessageItem("Bob", "Check out this new project!", "Yesterday"),
        MessageItem("Charlie", "Can you join the call?", "2 days ago")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Inbox",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPlum,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(messages) { message ->
            MessageCard(message)
        }
    }
}

@Composable
fun MessageCard(message: MessageItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = "Message",
                tint = RoyalViolet,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.sender,
                    fontWeight = FontWeight.Bold,
                    color = DeepPlum,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    color = SteelGray,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            Text(
                text = message.timestamp,
                color = SteelGray,
                fontSize = 11.sp
            )
        }
    }
}
