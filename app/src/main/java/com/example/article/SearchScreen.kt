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
import com.example.article.Repository.Neighbourhood
import com.example.article.Repository.NeighbourhoodSearchState
import com.example.article.Repository.NeighbourhoodSearchViewModel
import com.example.article.Repository.SearchUiState
import com.example.article.Repository.SearchViewModel
import com.example.article.Repository.SearchableUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    searchViewModel: SearchViewModel = viewModel(),
    neighbourhoodViewModel: NeighbourhoodSearchViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedCategory by searchViewModel.selectedCategory.collectAsState()

    // Neighbourhood state
    val neighbourhoodState by neighbourhoodViewModel.uiState.collectAsState()
    val requestStatusMap by neighbourhoodViewModel.requestStatusMap.collectAsState()
    val actionMessage by neighbourhoodViewModel.actionMessage.collectAsState()
    val isActioning by neighbourhoodViewModel.isActioning.collectAsState()

    // User state
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val alreadyMember = currentUser?.neighbourhood?.isNotEmpty() == true

    // User search state
    val userUiState by searchViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Categories now include Neighbourhoods
    val categories = remember { listOf("All", "Members", "Providers", "Neighbourhoods") }

    // Determine if Neighbourhoods tab is active
    val isNeighbourhoodTab = selectedCategory == "Neighbourhoods"

    LaunchedEffect(Unit) {
        searchViewModel.loadUsers()
        neighbourhoodViewModel.loadNeighbourhoods()
    }

    // Only apply user search when not on neighbourhood tab
    LaunchedEffect(searchQuery, isNeighbourhoodTab) {
        if (!isNeighbourhoodTab) {
            searchViewModel.searchUsers(searchQuery)
        } else {
            neighbourhoodViewModel.searchNeighbourhoods(searchQuery)
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            neighbourhoodViewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            colors = listOf(Color(0xFF42A5F5), Color(0xFF4DD0E1))
                        )
                    )
                    .shadow(elevation = 4.dp, spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f))
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
            // Search Bar — placeholder text adapts to active tab
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text(
                        text = if (isNeighbourhoodTab)
                            "Search neighbourhoods..."
                        else
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

            // Category Chips — now includes Neighbourhoods
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            searchViewModel.setCategory(category)
                            searchQuery = "" // clear search when switching tabs
                        },
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

            // Content — switches between user list and neighbourhood list
            if (isNeighbourhoodTab) {
                NeighbourhoodTabContent(
                    state = neighbourhoodState,
                    requestStatusMap = requestStatusMap,
                    isActioning = isActioning,
                    alreadyMember = alreadyMember,
                    currentUserNeighbourhood = currentUser?.neighbourhood ?: "",
                    onRequestJoin = { neighbourhoodViewModel.sendJoinRequest(it) },
                    onRetry = { neighbourhoodViewModel.loadNeighbourhoods() }
                )
            } else {
                UserTabContent(
                    state = userUiState,
                    onRetry = { searchViewModel.loadUsers() },
                    onUserClick = { user ->
                        navController.navigate("view_profile/${user.uid}/${user.role}")
                    }
                )
            }
        }
    }
}

// =====================================================================
// NEIGHBOURHOOD TAB CONTENT
// =====================================================================

@Composable
private fun NeighbourhoodTabContent(
    state: NeighbourhoodSearchState,
    requestStatusMap: Map<String, String>,
    isActioning: Boolean,
    alreadyMember: Boolean,
    currentUserNeighbourhood: String,
    onRequestJoin: (Neighbourhood) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        NeighbourhoodSearchState.Idle,
        NeighbourhoodSearchState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF42A5F5))
            }
        }

        is NeighbourhoodSearchState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFCC4444)
                    )
                    Text(state.message, color = Color(0xFFCC4444), fontSize = 14.sp)
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
                    ) { Text("Retry") }
                }
            }
        }

        is NeighbourhoodSearchState.Success -> {
            if (state.neighbourhoods.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No neighbourhoods found",
                    subtitle = "Try a different search term"
                )
            } else {
                // Already a member banner
                if (alreadyMember) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "You're already a member of: $currentUserNeighbourhood",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    text = "${state.neighbourhoods.size} neighbourhood${if (state.neighbourhoods.size != 1) "s" else ""} available",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.neighbourhoods, key = { it.id }) { neighbourhood ->
                        val requestStatus = requestStatusMap[neighbourhood.id] ?: "none"
                        NeighbourhoodCard(
                            neighbourhood = neighbourhood,
                            requestStatus = requestStatus,
                            isActioning = isActioning,
                            isAlreadyMember = alreadyMember,
                            onRequestJoin = { onRequestJoin(neighbourhood) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeighbourhoodCard(
    neighbourhood: Neighbourhood,
    requestStatus: String,
    isActioning: Boolean,
    isAlreadyMember: Boolean,
    onRequestJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name + description
            Text(
                text = neighbourhood.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1a1a1a)
            )
            if (neighbourhood.description.isNotEmpty()) {
                Text(
                    text = neighbourhood.description,
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    maxLines = 2
                )
            }

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF888888)
                    )
                    Text(
                        "${neighbourhood.memberCount} Members",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF888888)
                    )
                    Text(
                        "${neighbourhood.providerCount} Providers",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            // Join button — 4 states
            when {
                requestStatus == "approved" || isAlreadyMember -> {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Member",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                requestStatus == "pending" -> {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HourglassEmpty, null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Request Sent — Pending",
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                else -> {
                    // "none" or "rejected" both show the button
                    Button(
                        onClick = onRequestJoin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isActioning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF42A5F5)
                        )
                    ) {
                        if (isActioning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (requestStatus == "rejected") "Request to Join Again"
                            else "Request to Join",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// USER TAB CONTENT (extracted from original SearchScreen — unchanged)
// =====================================================================

@Composable
private fun UserTabContent(
    state: SearchUiState,
    onRetry: () -> Unit,
    onUserClick: (SearchableUser) -> Unit
) {
    when (state) {
        SearchUiState.Idle -> EmptyState(
            icon = Icons.Default.Search,
            title = "Start Searching",
            subtitle = "Find members and service providers in your community"
        )

        SearchUiState.Loading -> LoadingState(message = "Loading users...")

        is SearchUiState.Error -> ErrorState(
            message = state.message,
            onRetry = onRetry
        )

        is SearchUiState.Success -> {
            if (state.users.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No results found",
                    subtitle = "Try a different search or category"
                )
            } else {
                Text(
                    text = "${state.users.size} result${if (state.users.size != 1) "s" else ""} found",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.users, key = { it.uid }) { user ->
                        UserCard(user = user, onClick = { onUserClick(user) })
                    }
                }
            }
        }
    }
}

// =====================================================================
// USER CARD (unchanged from your original)
// =====================================================================

@Composable
private fun UserCard(user: SearchableUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

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
                    Text(text = user.serviceType, fontSize = 12.sp, color = Color(0xFF666666))
                } else if (user.neighbourhood.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF666666)
                        )
                        Text(text = user.neighbourhood, fontSize = 12.sp, color = Color(0xFF666666))
                    }
                }
                if (user.role == "service_provider" && user.rating > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star, null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = user.rating.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1a1a1a)
                        )
                        Text("•", fontSize = 11.sp, color = Color(0xFF666666))
                        Text("${user.completedJobs} jobs", fontSize = 11.sp, color = Color(0xFF666666))
                    }
                }
            }

            Surface(
                color = when (user.role) {
                    "service_provider" -> if (user.isAvailable) Color(0xFF4CAF50).copy(alpha = 0.15f)
                    else Color(0xFFB71C1C).copy(alpha = 0.15f)
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
                        "service_provider" -> if (user.isAvailable) Color(0xFF4CAF50) else Color(0xFFB71C1C)
                        "admin" -> Color(0xFFFF9800)
                        else -> Color(0xFF42A5F5)
                    }
                )
            }
        }
    }
}