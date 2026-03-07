package com.example.article.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx. compose. material3.TabRowDefaults. tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.Neighbourhood
import com.example.article.Repository.NeighbourhoodSearchState
import com.example.article.Repository.ProviderNeighbourhoodSearchViewModel
import com.example.article.Repository.ServiceRequest
import com.example.article.Repository.ServiceRequestRepository
import com.example.article.ui.theme.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// ─── Local sealed state for the requests tab ─────────────────────────────────

private sealed class RequestSearchState {
    object Idle : RequestSearchState()
    object Loading : RequestSearchState()
    data class Success(val requests: List<ServiceRequest>) : RequestSearchState()
    data class Error(val message: String) : RequestSearchState()
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSearchScreen(
    neighbourhoodViewModel: ProviderNeighbourhoodSearchViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // ── Neighbourhood tab state ───────────────────────────────────────────
    val neighbourhoodState by neighbourhoodViewModel.uiState.collectAsState()
    val requestStatusMap by neighbourhoodViewModel.requestStatusMap.collectAsState()
    val actionMessage by neighbourhoodViewModel.actionMessage.collectAsState()
    val isActioning by neighbourhoodViewModel.isActioning.collectAsState()

    // ── Requests tab state ────────────────────────────────────────────────
    var requestSearchState by remember { mutableStateOf<RequestSearchState>(RequestSearchState.Idle) }
    var selectedServiceType by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repository = remember { ServiceRequestRepository() }

    val snackbarHostState = remember { SnackbarHostState() }

    val serviceTypes = listOf(
        "All", "Plumber", "Electrician", "Cleaner", "Carpenter", "Painter",
        "Gardener", "AC Repair", "Appliance Repair", "Pest Control",
        "Locksmith", "Handyman", "Mason", "Welder", "Tailor",
        "Beautician", "Tutor", "Chef/Cook", "Driver", "Security Guard",
        "Moving & Packing", "Interior Designer", "Other"
    )

    // Load neighbourhoods once on entry
    LaunchedEffect(Unit) {
        neighbourhoodViewModel.loadNeighbourhoods()
    }

    // Reload requests whenever the requests tab is active or the filter changes
    LaunchedEffect(selectedTab, selectedServiceType) {
        if (selectedTab == 1) {
            requestSearchState = RequestSearchState.Loading
            scope.launch {
                try {
                    repository.getPendingRequests(
                        if (selectedServiceType == "All") null else selectedServiceType
                    )
                        .catch { e -> requestSearchState = RequestSearchState.Error(e.message ?: "Error") }
                        .collect { list -> requestSearchState = RequestSearchState.Success(list) }
                } catch (e: Exception) {
                    requestSearchState = RequestSearchState.Error(e.message ?: "Failed to load")
                }
            }
        }
    }

    // Keep neighbourhood search query in sync
    LaunchedEffect(searchQuery) {
        if (selectedTab == 0) neighbourhoodViewModel.searchNeighbourhoods(searchQuery)
    }

    // Snackbar for join-request feedback
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
                        "Search",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BlueOnPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(BluePrimary, BlueSecondary)))
                    .shadow(6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {

            // ── Tab row ───────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BluePrimary
                    )
                }
            ) {
                listOf(
                    Icons.Default.Home to "Neighbourhoods",
                    Icons.Default.Search to "Requests"
                ).forEachIndexed { index, (icon, label) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            searchQuery = ""
                        },
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == index) BluePrimary else Color(0xFF999999)
                            )
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) BluePrimary else Color(0xFF999999)
                            )
                        }
                    }
                }
            }

            // ── Search bar (Neighbourhoods tab only) ──────────────────────
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = {
                        Text("Search neighbourhoods…", fontSize = 13.sp, color = Color(0xFF999999))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp), tint = Color(0xFF999999))
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = BluePrimary.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // ── Tab content ───────────────────────────────────────────────
            when (selectedTab) {
                0 -> NeighbourhoodTab(
                    state = neighbourhoodState,
                    requestStatusMap = requestStatusMap,
                    isActioning = isActioning,
                    onRequestJoin = { neighbourhoodViewModel.sendJoinRequest(it) },
                    onRetry = { neighbourhoodViewModel.loadNeighbourhoods() }
                )
                1 -> RequestsTab(
                    state = requestSearchState,
                    serviceTypes = serviceTypes,
                    selectedServiceType = selectedServiceType,
                    onSelectType = { selectedServiceType = if (it == "All") null else it },
                    onRetry = {
                        // Toggle to re-trigger the LaunchedEffect
                        val cur = selectedServiceType
                        selectedServiceType = if (cur == null) "All" else null
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Neighbourhoods Tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NeighbourhoodTab(
    state: NeighbourhoodSearchState,
    requestStatusMap: Map<String, String>,
    isActioning: String?,
    onRequestJoin: (Neighbourhood) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is NeighbourhoodSearchState.Idle,
        is NeighbourhoodSearchState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
        }

        is NeighbourhoodSearchState.Error -> {
            SearchEmptyState(
                icon = Icons.Default.ErrorOutline,
                iconTint = Color(0xFFCC4444),
                title = "Something went wrong",
                subtitle = state.message,
                action = {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) { Text("Retry") }
                }
            )
        }

        is NeighbourhoodSearchState.Success -> {
            if (state.neighbourhoods.isEmpty()) {
                SearchEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No neighbourhoods found",
                    subtitle = "Try a different search term"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${state.neighbourhoods.size} neighbourhood${if (state.neighbourhoods.size != 1) "s" else ""} available",
                            fontSize = 12.sp,
                            color = Color(0xFF888888),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(state.neighbourhoods, key = { it.id }) { neighbourhood ->
                        ProviderNeighbourhoodCard(
                            neighbourhood = neighbourhood,
                            requestStatus = requestStatusMap[neighbourhood.id] ?: "none",
                            isActioning = isActioning == neighbourhood.id,
                            onRequestJoin = { onRequestJoin(neighbourhood) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderNeighbourhoodCard(
    neighbourhood: Neighbourhood,
    requestStatus: String,
    isActioning: Boolean,
    onRequestJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name + "Joined" badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(neighbourhood.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                    if (neighbourhood.description.isNotEmpty()) {
                        Text(neighbourhood.description, fontSize = 13.sp, color = Color(0xFF666666), maxLines = 2)
                    }
                }
                if (requestStatus == "approved") {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(12.dp), Color(0xFF4CAF50))
                            Text("Joined", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(13.dp), Color(0xFF888888))
                    Text("${neighbourhood.memberCount} Members", fontSize = 12.sp, color = Color(0xFF888888))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, null, Modifier.size(13.dp), Color(0xFF888888))
                    Text("${neighbourhood.providerCount} Providers", fontSize = 12.sp, color = Color(0xFF888888))
                }
            }

            // Action area
            when (requestStatus) {
                "approved" -> {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.07f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(15.dp), Color(0xFF4CAF50))
                            Spacer(Modifier.width(6.dp))
                            Text("You're a provider here", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                        }
                    }
                }

                "pending" -> {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HourglassEmpty, null, Modifier.size(15.dp), Color(0xFFFF9800))
                            Spacer(Modifier.width(6.dp))
                            Text("Request Sent — Pending", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF9800))
                        }
                    }
                }

                else -> {
                    // "none" or "rejected"
                    Button(
                        onClick = onRequestJoin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isActioning,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (isActioning) {
                            CircularProgressIndicator(Modifier.size(14.dp), Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (requestStatus == "rejected") "Request to Join Again" else "Request to Join",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Requests Tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RequestsTab(
    state: RequestSearchState,
    serviceTypes: List<String>,
    selectedServiceType: String?,
    onSelectType: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Service type filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(serviceTypes) { type ->
                val isSelected = (type == "All" && selectedServiceType == null) || type == selectedServiceType
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectType(type) },
                    label = {
                        Text(
                            type,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                        selectedLabelColor = BluePrimary,
                        selectedLeadingIconColor = BluePrimary
                    )
                )
            }
        }

        when (state) {
            is RequestSearchState.Idle -> {
                SearchEmptyState(
                    icon = Icons.Default.Search,
                    title = "Browse Service Requests",
                    subtitle = "Select a service type above to find open requests"
                )
            }

            is RequestSearchState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            }

            is RequestSearchState.Error -> {
                SearchEmptyState(
                    icon = Icons.Default.ErrorOutline,
                    iconTint = Color(0xFFCC4444),
                    title = "Failed to load requests",
                    subtitle = state.message,
                    action = {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) { Text("Retry") }
                    }
                )
            }

            is RequestSearchState.Success -> {
                if (state.requests.isEmpty()) {
                    SearchEmptyState(
                        icon = Icons.Default.Inbox,
                        title = "No open requests",
                        subtitle = if (selectedServiceType != null)
                            "No pending requests for '$selectedServiceType' right now"
                        else "No pending requests available right now"
                    )
                } else {
                    Text(
                        "${state.requests.size} open request${if (state.requests.size != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.requests, key = { it.id }) { request ->
                            SearchRequestCard(request)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRequestCard(request: ServiceRequest) {
    val timeAgo = remember(request.createdAt) {
        val diff = System.currentTimeMillis() - request.createdAt.toDate().time
        when {
            diff < 3_600_000  -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else              -> "${diff / 86_400_000}d ago"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(BluePrimary.copy(alpha = 0.6f))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Avatar
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = BluePrimary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    request.memberName.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(request.memberName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                            Surface(color = BluePrimary.copy(alpha = 0.10f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    request.serviceType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    // OPEN badge
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFF9800).copy(alpha = 0.12f)) {
                        Text(
                            "OPEN",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF9800),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                if (request.title.isNotEmpty()) {
                    Text(request.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                }

                if (request.description.isNotEmpty()) {
                    Text(
                        request.description,
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        lineHeight = 19.sp,
                        maxLines = 2
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (request.memberNeighborhood.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(13.dp), Color(0xFF999999))
                            Text(request.memberNeighborhood, fontSize = 12.sp, color = Color(0xFF999999))
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, null, Modifier.size(13.dp), Color(0xFF999999))
                        Text(timeAgo, fontSize = 12.sp, color = Color(0xFF999999))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared empty / error state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    iconTint: Color = Color(0xFF999999).copy(alpha = 0.5f),
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = iconTint)
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF666666))
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 13.sp, color = Color(0xFF999999), textAlign = TextAlign.Center)
            }
            action?.invoke()
        }
    }
}