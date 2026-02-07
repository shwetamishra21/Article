package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class ServiceRequest(
    val id: String = "",
    val serviceType: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val status: String = "Pending",
    val createdBy: String = "",
    val createdAt: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    onCreateNew: () -> Unit
) {
    var requests by remember { mutableStateOf<List<ServiceRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            error = "Not authenticated"
            loading = false
            return@LaunchedEffect
        }

        try {
            val snapshot = firestore.collection("service_requests")
                .whereEqualTo("createdBy", userId)
                .get()
                .await()

            requests = snapshot.documents.mapNotNull { doc ->
                try {
                    ServiceRequest(
                        id = doc.id,
                        serviceType = doc.getString("serviceType") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getString("date") ?: "",
                        status = doc.getString("status") ?: "Pending",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.createdAt }

            loading = false
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load requests"
            loading = false
        }
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
            when {
                loading -> {
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

                error != null -> {
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

                requests.isEmpty() -> {
                    EmptyRequestsState(onCreateNew)
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
                        items(requests, key = { it.id }) { request ->
                            RequestCard(request)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(request: ServiceRequest) {
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
                    modifier = Modifier.weight(1f)
                )

                // ✨ IMPROVED STATUS CHIP - Matches cancel button style
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (request.status) {
                        "Pending" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                        "Accepted" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        "Completed" -> Color(0xFF42A5F5).copy(alpha = 0.15f)
                        "Cancelled" -> Color(0xFFB71C1C).copy(alpha = 0.15f)
                        else -> Color(0xFF666666).copy(alpha = 0.15f)
                    },
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = request.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (request.status) {
                            "Pending" -> Color(0xFFFF9800)
                            "Accepted" -> Color(0xFF4CAF50)
                            "Completed" -> Color(0xFF42A5F5)
                            "Cancelled" -> Color(0xFFB71C1C)
                            else -> Color(0xFF666666)
                        }
                    )
                }
            }

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

                Text(
                    text = request.date,
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
            }

            Text(
                text = request.description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF666666),
                maxLines = 2
            )

            if (request.status == "Pending") {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { /* Cancel request */ },
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
}

@Composable
private fun EmptyRequestsState(onCreateNew: () -> Unit) {
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
                Text(text = "📋", fontSize = 40.sp)
            }

            Text(
                text = "No service requests",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Create your first request to get started",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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