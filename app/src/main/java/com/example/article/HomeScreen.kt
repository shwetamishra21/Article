package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.article.ui.theme.*

data class Post(
    val title: String = "",
    val content: String = "",
    val author: String = ""
)

@Composable
fun HomeScreen() {
    // Placeholder posts (no Firebase)
    var posts by remember {
        mutableStateOf(
            listOf(
                Post("Welcome!", "This is your first post.", "Admin"),
                Post("Sample Post", "Edit your content here.", "User")
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppWhite)
            .padding(16.dp)
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(posts) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(post.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("By: ${post.author}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(post.content)
                    }
                }
            }
        }
    }
}
