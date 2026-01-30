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

@Composable
fun RequestsScreen(
    onCreateNew: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var requests by remember { mutableStateOf<List<ServiceRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.background
        )
    )

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

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            if (!loading && requests.isEmpty()) {
                EmptyRequestsState(onCreateNew)
            }

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
                    "pending" -> {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onCancel() }
                        )
                    }

                    "accepted" -> {
                        Text(
                            text = "Mark Completed",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onComplete() }
                        )
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
        Text("No service requests yet", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a request to connect with local service providers",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateNew) {
            Text("Create Request")
        }
    }
}
