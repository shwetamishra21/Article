package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.article.ui.theme.*

data class SearchResult(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val category: String
)

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Technology", "Design", "Productivity", "Motivation", "Tutorial")

    val mockResults = listOf(
        SearchResult(1, "🚀 Advanced Kotlin Tips", "Master advanced Kotlin concepts", "KotlinExpert", "Technology"),
        SearchResult(2, "🎨 UI Design Principles", "Learn fundamental UI design principles", "DesignGuru", "Design")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🔍 Discover Content", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary)
                Text("Find articles, authors, and topics that inspire you", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search articles, authors, or topics") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Categories
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                ForgeCard(
                    modifier = Modifier.clickable { selectedCategory = category },
                ) {
                    Text(
                        category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (query.isEmpty()) {
                item {
                    ForgeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.TravelExplore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Start Exploring", fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                            Text("Search for articles, authors, or topics that interest you", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(mockResults) { result ->
                    ForgeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(result.title, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("By ${result.author}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(result.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
