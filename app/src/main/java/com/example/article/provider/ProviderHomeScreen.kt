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
import androidx.navigation.NavController
import com.example.article.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class ServiceRequest(
    val id: String,
    val memberName: String,
    val serviceType: String,
    val description: String,
    val preferredDate: Long,
    val status: RequestStatus,
    val timestamp: Long
)

enum class RequestStatus {
    PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderHomeScreen(
    navController: NavController,
    providerName: String = "Provider"
) {
    var requests by remember { mutableStateOf(listOf<ServiceRequest>()) }
    var selectedFilter by remember { mutableStateOf(RequestStatus.PENDING) }

    LaunchedEffect(Unit) {
        // TODO: Load requests from Firebase
        // For now using mock data
        requests = listOf(
            ServiceRequest(
                "1", "John Doe", "Plumbing", "Kitchen sink repair needed",
                System.currentTimeMillis() + 86400000, RequestStatus.PENDING, System.currentTimeMillis()
            ),
            ServiceRequest(
                "2", "Jane Smith", "Electrical", "Light fixture installation",
                System.currentTimeMillis() + 172800000, RequestStatus.ACCEPTED, System.currentTimeMillis()
            )
        )
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
                selectedTabIndex = RequestStatus.entries.indexOf(selectedFilter),
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                RequestStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        label = {
                            Text(
                                status.name.replace("_", " "),
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

            // Requests List
            if (filteredRequests.isEmpty()) {
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
                            "No ${selectedFilter.name.lowercase()} requests",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRequests, key = { it.id }) { request ->
                        RequestCard(request, navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(request: ServiceRequest, navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = BluePrimary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight,
        onClick = { /* TODO: Navigate to request details */ }
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
                                text = request.memberName.first().uppercase(),
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

            // Description
            Text(
                text = request.description,
                fontSize = 14.sp,
                color = OnSurfaceLight.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )

            // Date Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.CalendarToday,
                    text = formatDate(request.preferredDate)
                )
                InfoChip(
                    icon = Icons.Default.AccessTime,
                    text = formatTimestamp(request.timestamp)
                )
            }

            // Action Buttons
            if (request.status == RequestStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Decline */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Decline", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { /* TODO: Accept */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Accept", fontSize = 14.sp)
                    }
                }
            } else if (request.status == RequestStatus.ACCEPTED) {
                Button(
                    onClick = { /* TODO: Mark in progress */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Start Work", fontSize = 14.sp)
                }
            } else if (request.status == RequestStatus.IN_PROGRESS) {
                Button(
                    onClick = { /* TODO: Mark complete */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Mark Complete", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: RequestStatus) {
    val (color, text) = when (status) {
        RequestStatus.PENDING -> Color(0xFFFF9800) to "PENDING"
        RequestStatus.ACCEPTED -> BluePrimary to "ACCEPTED"
        RequestStatus.IN_PROGRESS -> Color(0xFF2196F3) to "IN PROGRESS"
        RequestStatus.COMPLETED -> Color(0xFF4CAF50) to "COMPLETED"
        RequestStatus.CANCELLED -> Color(0xFFD32F2F) to "CANCELLED"
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