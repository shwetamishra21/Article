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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*

data class SearchResult(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val category: String,
    val relevance: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Technology", "Design", "Productivity", "Motivation", "Tutorial")

    val mockResults = listOf(
        SearchResult(1, "🚀 Advanced Kotlin Tips", "Master advanced Kotlin programming concepts", "KotlinExpert", "Technology", 0.95f),
        SearchResult(2, "🎨 UI Design Principles", "Learn fundamental UI design principles", "DesignGuru", "Design", 0.92f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderMist)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SearchHeader()
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrightWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search articles, authors, or topics...") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = RoyalViolet
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalViolet,
                            focusedLabelColor = RoyalViolet,
                            cursorColor = RoyalViolet
                        ),
                        singleLine = true
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Card(
                            modifier = Modifier.clickable { selectedCategory = category },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) RoyalViolet else BrightWhite
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                category,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                color = if (isSelected) BrightWhite else DeepPlum
                            )
                        }
                    }
                }
            }

            if (query.isNotEmpty()) {
                items(mockResults) { result ->
                    SearchResultCard(result)
                }
            } else {
                item {
                    InitialSearchState()
                }
            }
        }
    }
}

@Composable
fun SearchHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RoyalViolet),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "🔍 Discover Content",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BrightWhite
            )
            Text(
                "Find articles, authors, and topics that inspire you",
                fontSize = 14.sp,
                color = PeachGlow
            )
        }
    }
}

@Composable
fun SearchResultCard(result: SearchResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BrightWhite),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                result.title,
                fontWeight = FontWeight.Bold,
                color = DeepPlum,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "By ${result.author}",
                fontSize = 12.sp,
                color = SteelGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                result.content,
                fontSize = 14.sp,
                color = SteelGray
            )
        }
    }
}

@Composable
fun InitialSearchState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BrightWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.TravelExplore,
                contentDescription = null,
                tint = RoyalViolet,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Start Exploring",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPlum,
                textAlign = TextAlign.Center
            )
            Text(
                "Search for articles, authors, or topics that interest you",
                fontSize = 14.sp,
                color = SteelGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
