package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ServiceProvider(
    val id: String,
    val name: String,
    val service: String,
    val rating: Float,
    val reviews: Int,
    val available: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Plumber", "Electrician", "Cleaner", "Carpenter", "Painter", "Gardener")

    val allProviders = remember {
        listOf(
            ServiceProvider("1", "Mike's Plumbing", "Plumber", 4.8f, 156, true),
            ServiceProvider("2", "Spark Electric", "Electrician", 4.9f, 203, true),
            ServiceProvider("3", "Clean Pro", "Cleaner", 4.7f, 89, false),
            ServiceProvider("4", "Wood Master", "Carpenter", 4.6f, 112, true),
            ServiceProvider("5", "Perfect Paint", "Painter", 4.8f, 145, true),
            ServiceProvider("6", "Green Garden", "Gardener", 4.7f, 98, true),
            ServiceProvider("7", "Fix-It Fast", "Plumber", 4.5f, 78, true),
            ServiceProvider("8", "Bright Homes", "Cleaner", 4.9f, 234, true)
        )
    }

    val filteredProviders = remember(searchQuery, selectedCategory) {
        allProviders.filter { provider ->
            val matchesSearch = searchQuery.isBlank() ||
                    provider.name.contains(searchQuery, ignoreCase = true) ||
                    provider.service.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || provider.service == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search Services",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF42A5F5),
                                Color(0xFF4DD0E1)
                            )
                        )
                    )
                    .shadow(
                        elevation = 4.dp,
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
                    )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF42A5F5).copy(alpha = 0.03f),
                            Color(0xFFFAFAFA)
                        )
                    )
                )
        ) {
            // ✨ PREMIUM SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text(
                        "Search for services or providers...",
                        fontSize = 13.sp,
                        color = Color(0xFF666666).copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF42A5F5),
                    unfocusedBorderColor = Color(0xFF42A5F5).copy(alpha = 0.2f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // ✨ CATEGORY CHIPS
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = if (selectedCategory == category) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF42A5F5).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF42A5F5),
                            selectedLeadingIconColor = Color(0xFF42A5F5)
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ✨ RESULTS COUNT
            if (filteredProviders.isNotEmpty()) {
                Text(
                    text = "${filteredProviders.size} provider${if (filteredProviders.size != 1) "s" else ""} found",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
            }

            // ✨ PROVIDER CARDS
            if (filteredProviders.isEmpty()) {
                EmptySearchState(searchQuery)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredProviders,
                        key = { it.id }
                    ) { provider ->
                        ProviderCard(provider)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: ServiceProvider) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to details */ },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF42A5F5).copy(alpha = 0.15f),
                                Color(0xFF4DD0E1).copy(alpha = 0.15f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (provider.service) {
                        "Plumber" -> Icons.Default.Build
                        "Electrician" -> Icons.Default.Bolt
                        "Cleaner" -> Icons.Default.CleaningServices
                        "Carpenter" -> Icons.Default.Handyman
                        "Painter" -> Icons.Default.FormatPaint
                        else -> Icons.Default.Grass
                    },
                    contentDescription = null,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = provider.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1a1a1a)
                )

                Text(
                    text = provider.service,
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = provider.rating.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1a1a1a)
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "${provider.reviews} reviews",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Badge
            Surface(
                color = if (provider.available)
                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                else
                    Color(0xFFB71C1C).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (provider.available) "Available" else "Busy",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (provider.available) Color(0xFF4CAF50) else Color(0xFFB71C1C)
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF42A5F5).copy(alpha = 0.15f),
                                Color(0xFF42A5F5).copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF42A5F5).copy(alpha = 0.6f)
                )
            }

            Text(
                text = if (query.isBlank())
                    "No providers available"
                else
                    "No results found",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1a1a1a)
            )

            Text(
                text = "Try a different search or category",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}