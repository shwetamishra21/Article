package com.example.article

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.MemberRequestViewModel
import com.example.article.Repository.ServiceRequest
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    onCreateNew: () -> Unit,
    viewModel: MemberRequestViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    val requests by viewModel.requests.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Filter tabs
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Active", "Completed")

    // Provider profile bottom sheet state
    var showProviderSheet by remember { mutableStateOf(false) }
    var selectedProviderIdForSheet by remember { mutableStateOf<String?>(null) }

    // Load member requests
    LaunchedEffect(userId) {
        userId?.let {
            viewModel.loadMemberRequests(it)
        }
    }

    // Auto-dismiss error after 3 seconds
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    val filteredRequests = when (selectedTab) {
        1 -> requests.filter { it.isActive }
        2 -> requests.filter { it.status == ServiceRequest.STATUS_COMPLETED }
        else -> requests
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Service Requests",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create request",
                    tint = Color.White
                )
            }
        },
        snackbarHost = {
            error?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(it)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Filter Tab Row ──────────────────────────────────────────
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        val count = when (index) {
                            1 -> requests.count { it.isActive }
                            2 -> requests.count { it.status == ServiceRequest.STATUS_COMPLETED }
                            else -> requests.size
                        }
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal
                                )
                                // Live count badge — only shown when there are requests
                                if (count > 0 && index > 0) {
                                    Surface(
                                        color = if (selectedTab == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTab == index)
                                                Color.White
                                            else
                                                Color(0xFF666666),
                                            modifier = Modifier.padding(
                                                horizontal = 7.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Content states ──────────────────────────────────────────
                when {
                    loading && requests.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    !loading && error != null && requests.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "⚠️", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Unable to load requests",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                error ?: "Unknown error",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    filteredRequests.isEmpty() -> {
                        EmptyRequestsState(onCreateNew = onCreateNew, tab = selectedTab)
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredRequests, key = { it.id }) { request ->
                                RequestCard(
                                    request = request,
                                    onCancel = { viewModel.cancelRequest(request.id) },
                                    onRate = { rating, review ->
                                        viewModel.rateRequest(request.id, rating, review)
                                    },
                                    onViewProvider = { providerId ->
                                        selectedProviderIdForSheet = providerId
                                        showProviderSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Provider profile bottom sheet ───────────────────────────────────────
    if (showProviderSheet && selectedProviderIdForSheet != null) {
        MemberProviderProfileSheet(
            providerId = selectedProviderIdForSheet!!,
            onDismiss = {
                showProviderSheet = false
                selectedProviderIdForSheet = null
            }
        )
    }
}

@Composable
private fun RequestCard(
    request: ServiceRequest,
    onCancel: () -> Unit,
    onRate: (Float, String) -> Unit,
    onViewProvider: (String) -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Title + status chip ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1a1a1a),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Status Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (request.status) {
                        ServiceRequest.STATUS_PENDING -> Color(0xFFFF9800).copy(alpha = 0.15f)
                        ServiceRequest.STATUS_ACCEPTED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ServiceRequest.STATUS_IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.15f)
                        ServiceRequest.STATUS_COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ServiceRequest.STATUS_CANCELLED -> Color(0xFFB71C1C).copy(alpha = 0.15f)
                        else -> Color(0xFF666666).copy(alpha = 0.15f)
                    },
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = when (request.status) {
                            ServiceRequest.STATUS_PENDING -> "Pending"
                            ServiceRequest.STATUS_ACCEPTED -> "Accepted"
                            ServiceRequest.STATUS_IN_PROGRESS -> "In Progress"
                            ServiceRequest.STATUS_COMPLETED -> "Completed"
                            ServiceRequest.STATUS_CANCELLED -> "Cancelled"
                            else -> request.status
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (request.status) {
                            ServiceRequest.STATUS_PENDING -> Color(0xFFFF9800)
                            ServiceRequest.STATUS_ACCEPTED -> Color(0xFF4CAF50)
                            ServiceRequest.STATUS_IN_PROGRESS -> Color(0xFF2196F3)
                            ServiceRequest.STATUS_COMPLETED -> Color(0xFF4CAF50)
                            ServiceRequest.STATUS_CANCELLED -> Color(0xFFB71C1C)
                            else -> Color(0xFF666666)
                        }
                    )
                }
            }

            // ── Service type + preferred date row ───────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = request.serviceType,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )

                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFF999999)
                )

                // Show preferred date or "ASAP"
                request.preferredDate?.let {
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(it.toDate()),
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                } ?: run {
                    Text(
                        text = "ASAP",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // ── Description ─────────────────────────────────────────────────
            Text(
                text = request.description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF666666),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ── Neighbourhood (if set) ──────────────────────────────────────
            if (request.memberNeighborhood.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF999999)
                    )
                    Text(
                        text = request.memberNeighborhood,
                        fontSize = 11.sp,
                        color = Color(0xFF999999)
                    )
                }
            }

            // ── Provider info (when assigned) ───────────────────────────────
            if (request.providerId != null && request.providerName != null) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )

                // Entire provider row is tappable — opens provider profile sheet
                Surface(
                    onClick = { onViewProvider(request.providerId) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = request.providerName.firstOrNull()?.uppercase() ?: "P",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Provider Assigned",
                                fontSize = 10.sp,
                                color = Color(0xFF999999)
                            )
                            Text(
                                text = request.providerName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1a1a1a)
                            )
                        }

                        // In Progress pill — shown when work has started
                        if (request.status == ServiceRequest.STATUS_IN_PROGRESS) {
                            Surface(
                                color = Color(0xFF2196F3).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Loop,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF2196F3)
                                    )
                                    Text(
                                        "In Progress",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2196F3),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // Tap hint chevron
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "View provider",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFCCCCCC)
                            )
                        }
                    }
                }
            }

            // ── Rating prompt: completed but not yet rated ──────────────────
            if (request.status == ServiceRequest.STATUS_COMPLETED && request.rating == null) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "How was the service?",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Button(
                        onClick = { showRatingDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Rate", fontSize = 12.sp)
                    }
                }
            }

            // ── Already rated: show star row ────────────────────────────────
            if (request.status == ServiceRequest.STATUS_COMPLETED && request.rating != null) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < request.rating.toInt())
                                Icons.Default.Star
                            else
                                Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (i < request.rating.toInt())
                                Color(0xFFFFC107)
                            else
                                Color(0xFFCCCCCC)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "You rated this",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }
                // Show review text if the member left one
                if (!request.review.isNullOrBlank()) {
                    Text(
                        text = "\"${request.review}\"",
                        fontSize = 12.sp,
                        color = Color(0xFF999999),
                        lineHeight = 17.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // ── Cancel button: pending OR accepted (not started yet) ──────────
            if (request.status == ServiceRequest.STATUS_PENDING ||
                request.status == ServiceRequest.STATUS_ACCEPTED) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showCancelDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Cancel Request",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB71C1C)
                        )
                    }
                }
            }
        }
    }

    // ── Cancel confirmation dialog ──────────────────────────────────────────
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Request", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (request.status == ServiceRequest.STATUS_ACCEPTED)
                        "A provider has already accepted this request. Cancelling will remove them from the job. Are you sure?"
                    else
                        "Are you sure you want to cancel this service request?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    }
                ) {
                    Text(
                        "Cancel Request",
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Request")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Rating dialog ───────────────────────────────────────────────────────
    if (showRatingDialog) {
        RatingDialog(
            providerName = request.providerName ?: "the provider",
            onDismiss = { showRatingDialog = false },
            onSubmit = { rating, review ->
                onRate(rating, review)
                showRatingDialog = false
            }
        )
    }
}

@Composable
private fun RatingDialog(
    providerName: String,
    onDismiss: () -> Unit,
    onSubmit: (Float, String) -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate $providerName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "How satisfied are you with the service?",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )

                // Star selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { i ->
                        IconButton(onClick = { selectedRating = i + 1 }) {
                            Icon(
                                imageVector = if (i < selectedRating)
                                    Icons.Default.Star
                                else
                                    Icons.Default.StarBorder,
                                contentDescription = "${i + 1} stars",
                                modifier = Modifier.size(36.dp),
                                tint = if (i < selectedRating)
                                    Color(0xFFFFC107)
                                else
                                    Color(0xFFCCCCCC)
                            )
                        }
                    }
                }

                if (selectedRating > 0) {
                    Text(
                        text = when (selectedRating) {
                            1 -> "😞 Poor"
                            2 -> "😐 Fair"
                            3 -> "🙂 Good"
                            4 -> "😊 Very Good"
                            else -> "🌟 Excellent!"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (selectedRating) {
                            1 -> Color(0xFFD32F2F)
                            2 -> Color(0xFFFF9800)
                            3 -> Color(0xFF2196F3)
                            4 -> Color(0xFF4CAF50)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Write a review (optional)", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRating > 0) onSubmit(selectedRating.toFloat(), reviewText)
                },
                enabled = selectedRating > 0,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun EmptyRequestsState(onCreateNew: () -> Unit, tab: Int = 0) {
    val (emoji, title, subtitle) = when (tab) {
        1 -> Triple("📋", "No active requests", "All your requests are resolved!")
        2 -> Triple("✅", "No completed requests yet", "Completed jobs will appear here")
        else -> Triple("📋", "No service requests", "Create your first request to get started")
    }

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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 40.sp)
            }

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (tab == 0) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onCreateNew,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Create Request",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}