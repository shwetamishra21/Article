package com.example.article.admin


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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.JoinRequest
import com.example.article.Repository.JoinRequestViewModel
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRequestsScreen(
    onNavigateBack: () -> Unit,
    viewModel: JoinRequestViewModel = viewModel()
) {
    val requests by viewModel.requests.collectAsState()
    val neighbourhood by viewModel.neighbourhood.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadForAdmin()
    }

    // Show snackbar for success or error messages
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Join Requests",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        neighbourhood?.let {
                            Text(
                                text = it.name,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BluePrimary
                    )
                }

                error != null && requests.isEmpty() -> {
                    // Only show full-screen error if we have no data at all
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFCC4444)
                        )
                        Text(
                            text = error ?: "Unknown error",
                            color = Color(0xFFCC4444),
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { viewModel.loadForAdmin() },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                requests.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF4CAF50).copy(alpha = 0.7f)
                        )
                        Text(
                            text = "All clear!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight
                        )
                        Text(
                            text = "No pending join requests right now.",
                            fontSize = 13.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "${requests.size} pending request${if (requests.size != 1) "s" else ""}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(
                            items = requests,
                            key = { it.id }
                        ) { request ->
                            JoinRequestCard(
                                request = request,
                                onApprove = { viewModel.approveRequest(request) },
                                onReject = { viewModel.rejectRequest(request) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinRequestCard(
    request: JoinRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var showConfirmReject by remember { mutableStateOf(false) }

    // Reject confirmation dialog
    if (showConfirmReject) {
        AlertDialog(
            onDismissRequest = { showConfirmReject = false },
            title = { Text("Reject Request") },
            text = { Text("Are you sure you want to reject ${request.userName}'s request?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmReject = false
                        onReject()
                    }
                ) {
                    Text("Reject", color = Color(0xFFCC4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReject = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = BluePrimary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User info row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with initials
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BluePrimary.copy(alpha = 0.2f),
                                    BlueSecondary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.userName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.userName.ifEmpty { "Unknown User" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = request.userEmail,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }

                // Role badge
                Surface(
                    color = when (request.userRole) {
                        "service_provider" -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                        else -> BluePrimary.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (request.userRole) {
                            "service_provider" -> "Provider"
                            else -> "Member"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (request.userRole) {
                            "service_provider" -> Color(0xFF4CAF50)
                            else -> BluePrimary
                        }
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reject button
                OutlinedButton(
                    onClick = { showConfirmReject = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCC4444)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFCC4444).copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Approve button
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Approve", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}