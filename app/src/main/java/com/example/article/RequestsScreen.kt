package com.example.article

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
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
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val status: String = "pending",
    val date: String = ""
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
        if (user == null) {
            loading = false
            return@DisposableEffect onDispose {}
        }

        val listener: ListenerRegistration =
            firestore.collection("requests")
                .whereEqualTo("createdBy", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        loading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        requests = snapshot.documents.mapNotNull { doc ->
                            try {
                                ServiceRequest(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    category = doc.getString("serviceType") ?: "",
                                    status = doc.getString("status") ?: "pending",
                                    date = doc.getString("date") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        loading = false
                    }
                }

        onDispose {
            try {
                listener.remove()
            } catch (e: Exception) {
                // Ignore disposal errors
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Requests",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.shadow(
                    elevation = 4.dp,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        },
        floatingActionButton = {
            // ✨ PREMIUM FAB WITH BLUE GLOW & ROTATION
            FloatingActionButton(
                onClick = onCreateNew,
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                ),
                modifier = Modifier.shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF42A5F5),
                                    Color(0xFF4DD0E1)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create request",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
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
                            Color(0xFF42A5F5).copy(alpha = 0.03f),
                            Color(0xFFFAFAFA)
                        )
                    )
                )
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp),
                        color = Color(0xFF42A5F5)
                    )
                }
            } else if (requests.isEmpty()) {
                EmptyRequestsState(onCreateNew)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
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
                                try {
                                    firestore.collection("requests")
                                        .document(request.id)
                                        .update("status", "cancelled")
                                } catch (e: Exception) {
                                    // Handle error silently
                                }
                            },
                            onComplete = {
                                try {
                                    firestore.collection("requests")
                                        .document(request.id)
                                        .update("status", "completed")
                                } catch (e: Exception) {
                                    // Handle error silently
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ---------- PREMIUM REQUEST CARD WITH BLUE GLOW ---------- */

@Composable
private fun RequestCard(
    request: ServiceRequest,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF42A5F5).copy(alpha = 0.08f),
                ambientColor = Color(0xFF42A5F5).copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFF42A5F5).copy(alpha = 0.08f)
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
                fontSize = 16.sp,
                color = Color(0xFF1a1a1a)
            )

            // Meta info
            Text(
                text = "${request.category} • ${request.date}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = Color(0xFF666666)
            )

            // ✨ PREMIUM GRADIENT DIVIDER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF42A5F5).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Status and Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChipPremium(request.status)

                when (request.status) {
                    "pending" -> {
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFB71C1C)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    "accepted" -> {
                        TextButton(
                            onClick = onComplete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF42A5F5)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Mark Complete",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    else -> {
                        // No action for completed or cancelled
                    }
                }
            }
        }
    }
}

/* ---------- PREMIUM STATUS CHIP WITH COLORED GLOW ---------- */

@Composable
private fun StatusChipPremium(status: String) {
    val (label, bgColor, textColor) = when (status) {
        "pending" -> Triple(
            "Pending",
            Color(0xFFFFA000).copy(alpha = 0.15f),
            Color(0xFFFFA000)
        )
        "accepted" -> Triple(
            "Accepted",
            Color(0xFF2E7D32).copy(alpha = 0.15f),
            Color(0xFF2E7D32)
        )
        "completed" -> Triple(
            "Completed",
            Color(0xFF1565C0).copy(alpha = 0.15f),
            Color(0xFF1565C0)
        )
        "cancelled" -> Triple(
            "Cancelled",
            Color(0xFFB71C1C).copy(alpha = 0.15f),
            Color(0xFFB71C1C)
        )
        else -> Triple(
            "Unknown",
            Color.Gray.copy(alpha = 0.15f),
            Color.Gray
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp),
            spotColor = textColor.copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/* ---------- PREMIUM EMPTY STATE WITH BLUE GLOW ---------- */

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
            // ✨ PREMIUM EMPTY ICON WITH BLUE GLOW
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF42A5F5).copy(alpha = 0.1f),
                                Color(0xFF4DD0E1).copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentLate,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF42A5F5).copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "No requests yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color(0xFF1a1a1a)
            )

            Text(
                text = "Create a request to connect with\nservice providers",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(16.dp))

            // ✨ PREMIUM GRADIENT BUTTON WITH GLOW
            Button(
                onClick = onCreateNew,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                ),
                modifier = Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.35f),
                        ambientColor = Color(0xFF42A5F5).copy(alpha = 0.25f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF42A5F5),
                                    Color(0xFF4DD0E1)
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Create Request",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}