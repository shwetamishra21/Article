package com.example.article.provider

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import com.example.article.Repository.Neighbourhood
import com.example.article.Repository.ProviderRequestsViewModel
import com.example.article.Repository.ServiceRequest
import com.example.article.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderHomeScreen(
    navController: NavController,
    viewModel: ProviderRequestsViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val providerId = auth.currentUser?.uid

    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val pendingLoading by viewModel.pendingLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val noNeighbourhood by viewModel.noNeighbourhood.collectAsState()
    val providerNeighbourhoods by viewModel.providerNeighbourhoods.collectAsState()

    var providerServiceType by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    // Load provider's own service type once (used as default filter)
    LaunchedEffect(providerId) {
        providerId ?: return@LaunchedEffect
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(providerId).get().await()
            providerServiceType = doc.getString("serviceType")
        } catch (_: Exception) {}
    }

    // Reload pending requests when filter or service type changes
    LaunchedEffect(selectedFilter, providerServiceType) {
        val effectiveFilter = selectedFilter ?: providerServiceType
        viewModel.loadPendingRequests(effectiveFilter)
    }

    // Auto-dismiss error after 3s
    LaunchedEffect(error) {
        if (error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    val serviceTypes = listOf(
        "All", "Plumber", "Electrician", "Cleaner", "Carpenter", "Painter",
        "Gardener", "AC Repair", "Appliance Repair", "Pest Control",
        "Locksmith", "Handyman", "Mason", "Welder", "Tailor",
        "Beautician", "Tutor", "Chef/Cook", "Driver", "Security Guard",
        "Moving & Packing", "Interior Designer", "Other"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Available Requests",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = BlueOnPrimary
                        )
                        // Subtitle: single name, count, or nothing
                        when {
                            providerNeighbourhoods.size == 1 -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        tint = BlueOnPrimary.copy(alpha = 0.75f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = providerNeighbourhoods.first().name,
                                        fontSize = 12.sp,
                                        color = BlueOnPrimary.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            providerNeighbourhoods.size > 1 -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        tint = BlueOnPrimary.copy(alpha = 0.75f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${providerNeighbourhoods.size} neighbourhoods",
                                        fontSize = 12.sp,
                                        color = BlueOnPrimary.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (!noNeighbourhood) {
                        BadgedBox(
                            badge = {
                                if (selectedFilter != null) {
                                    Badge(containerColor = Color.White) {
                                        Text("1", color = BluePrimary, fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = BlueOnPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(BluePrimary, BlueSecondary)))
                    .shadow(6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            when {
                // ── No neighbourhood: prompt to join one ─────────────────
                noNeighbourhood -> {
                    NoNeighbourhoodState(
                        onGoToSearch = {
                            navController.navigate("provider_search") {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // ── Loading ──────────────────────────────────────────────
                pendingLoading && pendingRequests.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(
                            color = BluePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // ── Empty feed ───────────────────────────────────────────
                !pendingLoading && pendingRequests.isEmpty() -> {
                    EmptyFeedState(
                        hasFilter = selectedFilter != null,
                        neighbourhoodNames = providerNeighbourhoods.map { it.name },
                        onClearFilter = { selectedFilter = null }
                    )
                }

                // ── Request feed ─────────────────────────────────────────
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (providerNeighbourhoods.isNotEmpty()) {
                            item {
                                NeighbourhoodContextPill(
                                    neighbourhoods = providerNeighbourhoods,
                                    count = pendingRequests.size
                                )
                            }
                        }

                        if (selectedFilter != null) {
                            item {
                                FilterActivePill(
                                    filter = selectedFilter!!,
                                    onClear = { selectedFilter = null }
                                )
                            }
                        }

                        items(pendingRequests, key = { it.id }) { request ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                ProviderRequestCard(
                                    request = request,
                                    onAccept = {
                                        providerId?.let { id -> viewModel.accept(request.id, id) }
                                    },
                                    onDecline = {},
                                    onComplete = {},
                                    onStartWork = {}
                                )
                            }
                        }
                    }
                }
            }

            // Error snackbar
            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error ?: "")
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Filter by Service Type", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                serviceTypes.forEach { type ->
                    val isSelected = if (type == "All") selectedFilter == null else selectedFilter == type
                    FilterOption(
                        label = type,
                        selected = isSelected,
                        onClick = {
                            selectedFilter = if (type == "All") null else type
                            showFilterSheet = false
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// No neighbourhood state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoNeighbourhoodState(onGoToSearch: () -> Unit) {
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
                shape = RoundedCornerShape(24.dp),
                color = BluePrimary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = BluePrimary.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "No Neighbourhood Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )

            Text(
                text = "Join a neighbourhood to start seeing service requests from your community members.",
                fontSize = 14.sp,
                color = Color(0xFF777777),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onGoToSearch,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Find a Neighbourhood",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F4FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = "Once your request is approved by a neighbourhood admin, you'll see member requests here.",
                        fontSize = 12.sp,
                        color = Color(0xFF444466),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Neighbourhood context pill — single name or "N neighbourhoods"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NeighbourhoodContextPill(
    neighbourhoods: List<Neighbourhood>,
    count: Int
) {
    Surface(
        color = BluePrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (neighbourhoods.size == 1)
                    neighbourhoods.first().name
                else
                    "${neighbourhoods.size} neighbourhoods",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BluePrimary,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = BluePrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$count request${if (count != 1) "s" else ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reused composables (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterActivePill(filter: String, onClear: () -> Unit) {
    Surface(
        color = BluePrimary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, null, Modifier.size(14.dp), BluePrimary)
            Text(
                text = "Showing: $filter",
                fontSize = 13.sp,
                color = BluePrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            Surface(onClick = onClear, color = BluePrimary, shape = RoundedCornerShape(10.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear filter",
                    modifier = Modifier.size(18.dp).padding(3.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) BluePrimary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) BluePrimary else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) BluePrimary else Color(0xFF1a1a1a)
            )
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyFeedState(
    hasFilter: Boolean,
    neighbourhoodNames: List<String>,
    onClearFilter: () -> Unit
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
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFF999999).copy(alpha = 0.4f)
            )
            Text(
                if (hasFilter) "No requests for this filter" else "No requests right now",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666)
            )
            Text(
                when {
                    hasFilter -> "Try clearing the filter to see all requests"
                    neighbourhoodNames.size == 1 ->
                        "No open requests from ${neighbourhoodNames.first()} members yet"
                    neighbourhoodNames.size > 1 ->
                        "No open requests from any of your ${neighbourhoodNames.size} neighbourhoods yet"
                    else -> "New service requests will appear here"
                },
                fontSize = 14.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
            if (hasFilter) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onClearFilter, shape = RoundedCornerShape(12.dp)) {
                    Text("Clear Filter")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Home-feed request card (accept only)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderRequestCard(
    request: ServiceRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onComplete: () -> Unit,
    onStartWork: () -> Unit
) {
    var accepting by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = BluePrimary.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.memberName.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                    }
                    Column {
                        Text(
                            request.memberName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Text(
                            request.serviceType,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.12f)
                ) {
                    Text(
                        "NEW",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (request.memberNeighborhood.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF999999)
                    )
                    Text(request.memberNeighborhood, fontSize = 12.sp, color = Color(0xFF999999))
                }
            }

            if (request.title.isNotEmpty()) {
                Text(
                    request.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceLight
                )
            }

            if (request.description.isNotEmpty()) {
                Text(
                    request.description,
                    fontSize = 13.sp,
                    color = OnSurfaceLight.copy(alpha = 0.75f),
                    lineHeight = 19.sp,
                    maxLines = 3
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))

            Button(
                onClick = { accepting = true; onAccept() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = !accepting,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (accepting) {
                    CircularProgressIndicator(Modifier.size(16.dp), Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Accepting…", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Accept Request", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}