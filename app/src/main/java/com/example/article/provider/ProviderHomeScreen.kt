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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.article.Repository.ProviderRequestsViewModel
import com.example.article.Repository.ServiceRequest
import com.example.article.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    // Provider's own service type (loaded from Firestore)
    var providerServiceType by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<String?>(null) } // null = All

    // Load provider service type once
    LaunchedEffect(providerId) {
        providerId ?: return@LaunchedEffect
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(providerId).get().await()
            providerServiceType = doc.getString("serviceType")
        } catch (_: Exception) {}
    }

    // Load pending requests whenever filter or service type changes
    LaunchedEffect(selectedFilter, providerServiceType) {
        // If no manual filter, default to provider's own service type
        val effectiveFilter = selectedFilter ?: providerServiceType
        viewModel.loadPendingRequests(effectiveFilter)
    }

    // Auto-clear errors
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
                        if (providerServiceType != null) {
                            Text(
                                text = providerServiceType!!,
                                fontSize = 13.sp,
                                color = BlueOnPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    // Filter icon — shows selected filter badge
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
                pendingLoading && pendingRequests.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(
                            color = BluePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                !pendingLoading && pendingRequests.isEmpty() -> {
                    EmptyFeedState(
                        hasFilter = selectedFilter != null,
                        onClearFilter = { selectedFilter = null }
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Active filter pill
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
                                        providerId?.let { id ->
                                            viewModel.accept(request.id, id)
                                        }
                                    },
                                    onDecline = { /* Provider can dismiss from feed */ },
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
                Text(
                    "Filter by Service Type",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(4.dp))

                serviceTypes.forEach { type ->
                    val isSelected = when {
                        type == "All" -> selectedFilter == null
                        else -> selectedFilter == type
                    }
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
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = BluePrimary
            )
            Text(
                text = "Showing: $filter",
                fontSize = 13.sp,
                color = BluePrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            Surface(
                onClick = onClear,
                color = BluePrimary,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear filter",
                    modifier = Modifier
                        .size(18.dp)
                        .padding(3.dp),
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyFeedState(hasFilter: Boolean, onClearFilter: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
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
                if (hasFilter) "Try clearing the filter to see all requests"
                else "New service requests will appear here",
                fontSize = 14.sp,
                color = Color(0xFF999999)
            )
            if (hasFilter) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onClearFilter,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Filter")
                }
            }
        }
    }
}