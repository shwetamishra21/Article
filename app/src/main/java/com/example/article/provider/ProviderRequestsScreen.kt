package com.example.article.provider

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.ProviderRequestsViewModel
import com.example.article.Repository.ServiceRequest
import com.example.article.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderRequestsScreen(
    viewModel: ProviderRequestsViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val providerId = auth.currentUser?.uid

    val requests by viewModel.requests.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedFilter by remember { mutableStateOf(ServiceRequest.STATUS_ACCEPTED) }

    // Load requests when screen opens
    LaunchedEffect(providerId) {
        providerId?.let {
            viewModel.loadRequests(it)
        }
    }

    // Show error snackbar
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    // Guard: not authenticated
    if (providerId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Not authenticated",
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp
            )
        }
        return
    }

    val filteredRequests = requests.filter { it.status == selectedFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Service Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BlueOnPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BluePrimary, BlueSecondary)
                        )
                    )
                    .shadow(
                        elevation = 6.dp,
                        spotColor = BluePrimary.copy(alpha = 0.4f)
                    )
            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Filter Chips
            ScrollableTabRow(
                selectedTabIndex = listOf(
                    ServiceRequest.STATUS_ACCEPTED,
                    ServiceRequest.STATUS_IN_PROGRESS,
                    ServiceRequest.STATUS_COMPLETED
                ).indexOf(selectedFilter),
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                listOf(
                    ServiceRequest.STATUS_ACCEPTED to "Accepted",
                    ServiceRequest.STATUS_IN_PROGRESS to "In Progress",
                    ServiceRequest.STATUS_COMPLETED to "Completed"
                ).forEach { (status, label) ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        label = {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedFilter == status) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = BluePrimary
                        )
                    )
                }
            }

            // Loading indicator
            if (loading && requests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
                return@Column
            }

            // Empty state
            if (!loading && filteredRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF999999).copy(alpha = 0.5f)
                        )
                        Text(
                            "No ${selectedFilter.replace("_", " ")} requests",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                // Requests list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRequests, key = { it.id }) { request ->
                        ProviderRequestCard(
                            request = request,
                            onStartWork = { viewModel.startWork(request.id) },
                            onComplete = { viewModel.complete(request.id, providerId) },
                            onDecline = { viewModel.decline(request.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderRequestCard(
    request: ServiceRequest,
    onStartWork: () -> Unit,
    onComplete: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = BluePrimary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = BluePrimary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.memberName.firstOrNull()?.uppercase() ?: "M",
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Column {
                        Text(
                            text = request.memberName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Text(
                            text = request.serviceType,
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                StatusBadge(request.status)
            }

            // Title
            if (request.title.isNotEmpty()) {
                Text(
                    text = request.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceLight
                )
            }

            // Description
            if (request.description.isNotEmpty()) {
                Text(
                    text = request.description,
                    fontSize = 14.sp,
                    color = OnSurfaceLight.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }

            // Date Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                request.preferredDate?.let {
                    InfoChip(
                        icon = Icons.Default.CalendarToday,
                        text = formatDate(it.toDate().time)
                    )
                }
                InfoChip(
                    icon = Icons.Default.AccessTime,
                    text = formatTimestamp(request.createdAt.toDate().time)
                )
            }

            // Action Buttons
            when (request.status) {
                ServiceRequest.STATUS_ACCEPTED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Decline", fontSize = 14.sp)
                        }
                        Button(
                            onClick = onStartWork,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("Start Work", fontSize = 14.sp)
                        }
                    }
                }

                ServiceRequest.STATUS_IN_PROGRESS -> {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mark Complete", fontSize = 14.sp)
                    }
                }

                ServiceRequest.STATUS_COMPLETED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Completed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                        request.completedAt?.let {
                            Text(
                                " • ${formatDate(it.toDate().time)}",
                                fontSize = 13.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        ServiceRequest.STATUS_PENDING -> Color(0xFFFF9800) to "PENDING"
        ServiceRequest.STATUS_ACCEPTED -> BluePrimary to "ACCEPTED"
        ServiceRequest.STATUS_IN_PROGRESS -> Color(0xFF2196F3) to "IN PROGRESS"
        ServiceRequest.STATUS_COMPLETED -> Color(0xFF4CAF50) to "COMPLETED"
        ServiceRequest.STATUS_CANCELLED -> Color(0xFFD32F2F) to "CANCELLED"
        else -> Color(0xFF999999) to status.uppercase()
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF666666)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}