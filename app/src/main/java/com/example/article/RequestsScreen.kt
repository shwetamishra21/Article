package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------- DATA MODEL ---------- */

data class ServiceRequest(
    val id: String,
    val title: String,
    val category: String,
    val status: RequestStatus,
    val date: String
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    COMPLETED,
    CANCELLED
}

/* ---------- SCREEN ---------- */

@Composable
fun RequestsScreen(
    onCreateNew: () -> Unit
) {
    var requests by remember {
        mutableStateOf(
            listOf(
                ServiceRequest("req_1", "Bathroom pipe leakage", "Plumber", RequestStatus.PENDING, "Today"),
                ServiceRequest("req_2", "Fan not working", "Electrician", RequestStatus.ACCEPTED, "Yesterday")
            )
        )
    }

    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {

        Column {

            /* ---------- HEADER ---------- */
            Text(
                text = "My Service Requests",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                modifier = Modifier.padding(16.dp)
            )

            if (requests.isEmpty()) {
                EmptyRequestsState(onCreateNew)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = requests,
                        key = { it.id }   // ✅ UNIQUE & STABLE
                    ) { request ->
                        RequestCard(
                            request = request,
                            onCancel = {
                                requests = requests.map {
                                    if (it.id == request.id)
                                        it.copy(status = RequestStatus.CANCELLED)
                                    else it
                                }
                            },
                            onComplete = {
                                requests = requests.map {
                                    if (it.id == request.id)
                                        it.copy(status = RequestStatus.COMPLETED)
                                    else it
                                }
                            }
                        )
                    }
                }
            }
        }

        /* ---------- FAB ---------- */
        FloatingActionButton(
            onClick = onCreateNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create request")
        }
    }
}

/* ---------- REQUEST CARD ---------- */

@Composable
private fun RequestCard(
    request: ServiceRequest,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                text = request.title,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${request.category} • ${request.date}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                StatusChip(request.status)

                when (request.status) {
                    RequestStatus.PENDING -> {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onCancel() }
                        )
                    }

                    RequestStatus.ACCEPTED -> {
                        Text(
                            text = "Mark Completed",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onComplete() }
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}

/* ---------- STATUS CHIP ---------- */

@Composable
private fun StatusChip(status: RequestStatus) {

    val (label, color) = when (status) {
        RequestStatus.PENDING -> "Pending" to Color(0xFFFFA000)
        RequestStatus.ACCEPTED -> "Accepted" to Color(0xFF2E7D32)
        RequestStatus.COMPLETED -> "Completed" to Color(0xFF1565C0)
        RequestStatus.CANCELLED -> "Cancelled" to Color(0xFFB71C1C)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/* ---------- EMPTY STATE ---------- */

@Composable
private fun EmptyRequestsState(
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No service requests yet",
            fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Create a request to connect with local service providers",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateNew) {
            Text("Create Request")
        }
    }
}
