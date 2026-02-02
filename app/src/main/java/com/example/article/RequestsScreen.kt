package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/* ---------- MODEL ---------- */

data class ServiceRequest(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val date: String
)

/* ---------- SCREEN ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    onCreateNew: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var requests by remember { mutableStateOf<List<ServiceRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    /* ---------- REALTIME LISTENER ---------- */
    DisposableEffect(Unit) {
        if (user == null) return@DisposableEffect onDispose {}

        val listener: ListenerRegistration =
            firestore.collection("requests")
                .whereEqualTo("createdBy", user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        requests = snapshot.documents.mapNotNull { doc ->
                            ServiceRequest(
                                id = doc.id,
                                title = doc.getString("title") ?: return@mapNotNull null,
                                category = doc.getString("serviceType") ?: "",
                                status = doc.getString("status") ?: "pending",
                                date = doc.getString("date") ?: ""
                            )
                        }
                        loading = false
                    }
                }

        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Requests",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create request",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (requests.isEmpty()) {
                EmptyRequestsState(onCreateNew)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = requests,
                        key = { it.id }
                    ) { request ->
                        RequestCard(
                            request = request,
                            onCancel = {
                                firestore.collection("requests")
                                    .document(request.id)
                                    .update("status", "cancelled")
                            },
                            onComplete = {
                                firestore.collection("requests")
                                    .document(request.id)
                                    .update("status", "completed")
                            }
                        )
                    }
                }
            }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Meta info
            Text(
                text = "${request.category} • ${request.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Status and Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(request.status)

                when (request.status) {
                    "pending" -> {
                        TextButton(onClick = onCancel) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    "accepted" -> {
                        TextButton(onClick = onComplete) {
                            Text(
                                text = "Mark Complete",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------- STATUS CHIP ---------- */

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "pending" -> "Pending" to Color(0xFFFFA000)
        "accepted" -> "Accepted" to Color(0xFF2E7D32)
        "completed" -> "Completed" to Color(0xFF1565C0)
        "cancelled" -> "Cancelled" to Color(0xFFB71C1C)
        else -> "Unknown" to Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/* ---------- EMPTY STATE ---------- */


@Composable
private fun EmptyRequestsState(onCreateNew: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentLate,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "No requests yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Create a request to connect with service providers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onCreateNew,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Request")
            }
        }
    }
}