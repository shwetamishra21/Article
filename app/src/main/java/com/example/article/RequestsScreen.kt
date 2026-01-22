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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*

/* ---------- DATA MODEL (UI ONLY) ---------- */

data class ServiceRequest(
    val id: String,
    val title: String,
    val category: String,
    val status: RequestStatus,
    val date: String
)

enum class RequestStatus {
    PENDING, ACCEPTED, CANCELLED
}

/* ---------- MAIN SCREEN ---------- */

@Composable
fun RequestsScreen(
    onCreateNew: () -> Unit = {}
) {
    var requests by remember {
        mutableStateOf(
            listOf(
                ServiceRequest("1", "Bathroom pipe leakage", "Plumber", RequestStatus.PENDING, "Today"),
                ServiceRequest("2", "Fan not working", "Electrician", RequestStatus.ACCEPTED, "Yesterday")
            )
        )
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            /* ---------- HEADER ---------- */
            Text(
                text = "My Service Requests",
                fontSize = 22.sp,
                style = MaterialTheme.typography.titleLarge,
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
                    items(requests, key = { it.id }) { request ->
                        RequestCard(
                            request = request,
                            onCancel = {
                                requests = requests.map {
                                    if (it.id == request.id)
                                        it.copy(status = RequestStatus.CANCELLED)
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
            Icon(Icons.Default.Add, contentDescription = "New Request")
        }
    }
}

/* ---------- REQUEST CARD ---------- */

@Composable
private fun RequestCard(
    request: ServiceRequest,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = request.title,
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${request.category} • ${request.date}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusChip(request.status)

                if (request.status == RequestStatus.PENDING) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { onCancel() }
                    )
                }
            }
        }
    }
}

/* ---------- STATUS CHIP ---------- */

@Composable
private fun StatusChip(status: RequestStatus) {

    val label: String
    val color: androidx.compose.ui.graphics.Color

    when (status) {
        RequestStatus.PENDING -> {
            label = "Pending"
            color = androidx.compose.ui.graphics.Color(0xFFFFA000)
        }
        RequestStatus.ACCEPTED -> {
            label = "Accepted"
            color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
        }
        RequestStatus.CANCELLED -> {
            label = "Cancelled"
            color = androidx.compose.ui.graphics.Color(0xFFB71C1C)
        }
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create a request to connect with local service providers",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateNew) {
            Text("Create Request")
        }
    }
}
