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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.article.Repository.SearchUiState
import com.example.article.Repository.SearchViewModel
import com.example.article.Repository.SearchableUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    // Load users on first composition
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    // Apply search whenever query changes
    LaunchedEffect(searchQuery) {
        viewModel.searchUsers(searchQuery)
    }

    // Get categories - simplified to always show base categories
    val categories = remember {
        listOf("All", "Members", "Providers")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search",
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text(
                        "Search for people or services...",
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

            // Category Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
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

            // Content
            when (val state = uiState) {
                SearchUiState.Idle -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "Start Searching",
                        subtitle = "Find members and service providers in your community"
                    )
                }

                SearchUiState.Loading -> {
                    LoadingState(message = "Loading users...")
                }

                is SearchUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadUsers() }
                    )
                }

                is SearchUiState.Success -> {
                    if (state.users.isNotEmpty()) {
                        Text(
                            text = "${state.users.size} result${if (state.users.size != 1) "s" else ""} found",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (state.users.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "No results found",
                            subtitle = "Try a different search or category"
                        )
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
                                items = state.users,
                                key = { it.uid }
                            ) { user ->
                                UserCard(
                                    user = user,
                                    onClick = {
                                        // Navigate to profile view
                                        navController.navigate("view_profile/${user.uid}/${user.role}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: SearchableUser,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                if (user.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = user.name.ifEmpty { "User" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1a1a1a)
                )

                if (user.role == "service_provider" && user.serviceType.isNotEmpty()) {
                    Text(
                        text = user.serviceType,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                } else if (user.neighbourhood.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF666666)
                        )
                        Text(
                            text = user.neighbourhood,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                // Provider rating
                if (user.role == "service_provider" && user.rating > 0) {
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
                            text = user.rating.toString(),
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
                            text = "${user.completedJobs} jobs",
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            // Role Badge
            Surface(
                color = when (user.role) {
                    "service_provider" -> {
                        if (user.isAvailable)
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else
                            Color(0xFFB71C1C).copy(alpha = 0.15f)
                    }
                    "admin" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                    else -> Color(0xFF42A5F5).copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when (user.role) {
                        "service_provider" -> if (user.isAvailable) "Available" else "Busy"
                        "admin" -> "Admin"
                        else -> "Member"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (user.role) {
                        "service_provider" -> {
                            if (user.isAvailable) Color(0xFF4CAF50) else Color(0xFFB71C1C)
                        }
                        "admin" -> Color(0xFFFF9800)
                        else -> Color(0xFF42A5F5)
                    }
                )
            }
        }
    }
}