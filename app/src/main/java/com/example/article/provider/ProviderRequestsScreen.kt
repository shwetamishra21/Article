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
import androidx.compose.ui.text.font.FontStyle
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

    val requests        by viewModel.requests.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val loading         by viewModel.loading.collectAsState()
    val pendingLoading  by viewModel.pendingLoading.collectAsState()
    val error           by viewModel.error.collectAsState()

    // 0=New(Pending), 1=Accepted, 2=In Progress, 3=Completed
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(providerId) {
        providerId?.let {
            viewModel.loadRequests(it)
            viewModel.loadPendingRequests()
        }
    }

    error?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    if (providerId == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Not authenticated", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
        }
        return
    }

    val acceptedList   = requests.filter { it.status == ServiceRequest.STATUS_ACCEPTED }
    val inProgressList = requests.filter { it.status == ServiceRequest.STATUS_IN_PROGRESS }
    val completedList  = requests.filter { it.status == ServiceRequest.STATUS_COMPLETED }

    val filteredRequests = when (selectedTab) {
        0    -> pendingRequests
        1    -> acceptedList
        2    -> inProgressList
        3    -> completedList
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Service Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BlueOnPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(BluePrimary, BlueSecondary)))
                    .shadow(6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        },
        snackbarHost = {
            error?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) { Text(it) }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {

            // ── Tab Row ─────────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                listOf(
                    "New"         to pendingRequests.size,
                    "Accepted"    to acceptedList.size,
                    "In Progress" to inProgressList.size,
                    "Completed"   to completedList.size
                ).forEachIndexed { index, (label, count) ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold
                                    else FontWeight.Medium
                                )
                                if (count > 0) {
                                    Surface(
                                        color = if (selectedTab == index) BluePrimary
                                        else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTab == index) Color.White
                                            else Color(0xFF666666),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                            selectedLabelColor     = BluePrimary
                        )
                    )
                }
            }

            // ── Loading ─────────────────────────────────────────────────────
            val isLoading = if (selectedTab == 0) pendingLoading else loading
            if (isLoading && filteredRequests.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
                return@Column
            }

            // ── Empty state ─────────────────────────────────────────────────
            if (!isLoading && filteredRequests.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (selectedTab) {
                                0    -> Icons.Default.Inbox
                                1    -> Icons.Default.Assignment
                                2    -> Icons.Default.Loop
                                else -> Icons.Default.Done
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF999999).copy(alpha = 0.5f)
                        )
                        Text(
                            text = when (selectedTab) {
                                0    -> "No new requests"
                                1    -> "No accepted requests"
                                2    -> "No jobs in progress"
                                else -> "No completed jobs yet"
                            },
                            fontSize = 16.sp,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (selectedTab) {
                                0    -> "New service requests from members will appear here"
                                1    -> "Accept a request from the New tab to see it here"
                                2    -> "Begin an accepted job to move it here"
                                else -> "Completed jobs will appear here"
                            },
                            fontSize = 13.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }
            } else {
                // ── List ────────────────────────────────────────────────────
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        items(filteredRequests, key = { it.id }) { request ->
                            PendingRequestCard(
                                request  = request,
                                onAccept = {
                                    viewModel.accept(request.id, providerId)
                                    selectedTab = 1   // jump straight to Accepted tab
                                }
                            )
                        }
                    } else {
                        items(filteredRequests, key = { it.id }) { request ->
                            ProviderRequestCard(
                                request       = request,
                                onBeginWork   = {
                                    viewModel.startWork(request.id)
                                    selectedTab = 2   // jump to In Progress tab
                                },
                                onMarkComplete = {
                                    viewModel.complete(request.id, providerId)
                                    selectedTab = 3   // jump to Completed tab
                                },
                                onDecline     = {
                                    viewModel.decline(request.id)
                                    selectedTab = 0   // return to New tab
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// "New" tab card — accept only
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PendingRequestCard(
    request: ServiceRequest,
    onAccept: () -> Unit
) {
    var accepting by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    AvatarCircle(request.memberName, 40)
                    Column {
                        Text(
                            request.memberName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Text(
                            request.serviceType,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Text(
                        "NEW",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (request.memberNeighborhood.isNotBlank()) {
                IconText(Icons.Default.LocationOn, request.memberNeighborhood)
            }

            if (request.title.isNotEmpty()) {
                Text(request.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
            }

            if (request.description.isNotEmpty()) {
                Text(
                    request.description,
                    fontSize = 14.sp,
                    color = OnSurfaceLight.copy(alpha = 0.8f),
                    lineHeight = 20.sp,
                    maxLines = 3
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                request.preferredDate?.let {
                    InfoChip(Icons.Default.CalendarToday, formatDate(it.toDate().time))
                }
                InfoChip(Icons.Default.AccessTime, formatTimestamp(request.createdAt.toDate().time))
            }

            HorizontalDivider(thickness = 1.dp, color = BluePrimary.copy(alpha = 0.08f))

            // Accept button
            Button(
                onClick = { accepting = true; onAccept() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = !accepting
            ) {
                if (accepting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Accepting…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Accept Request",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Accepted / In Progress / Completed card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProviderRequestCard(
    request: ServiceRequest,
    onBeginWork: () -> Unit,
    onMarkComplete: () -> Unit,
    onDecline: () -> Unit
) {
    var showDeclineDialog by remember { mutableStateOf(false) }
    var beginningWork     by remember { mutableStateOf(false) }
    var completing        by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.3f)),
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
                    AvatarCircle(request.memberName, 40)
                    Column {
                        Text(
                            request.memberName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Text(
                            request.serviceType,
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                StatusBadge(request.status)
            }

            if (request.memberNeighborhood.isNotBlank()) {
                IconText(Icons.Default.LocationOn, request.memberNeighborhood)
            }

            if (request.title.isNotEmpty()) {
                Text(
                    request.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceLight
                )
            }

            if (request.description.isNotEmpty()) {
                Text(
                    request.description,
                    fontSize = 14.sp,
                    color = OnSurfaceLight.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }

            // Date / time chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                request.preferredDate?.let {
                    InfoChip(Icons.Default.CalendarToday, formatDate(it.toDate().time))
                }
                InfoChip(Icons.Default.AccessTime, formatTimestamp(request.createdAt.toDate().time))
                if (request.status == ServiceRequest.STATUS_IN_PROGRESS && request.acceptedAt != null) {
                    InfoChip(
                        Icons.Default.CheckCircle,
                        "Accepted ${formatTimestamp(request.acceptedAt.toDate().time)}"
                    )
                }
            }

            if (request.status != ServiceRequest.STATUS_COMPLETED) {
                HorizontalDivider(thickness = 1.dp, color = BluePrimary.copy(alpha = 0.08f))
            }

            // ── Action section per status ─────────────────────────────────
            when (request.status) {

                // ── ACCEPTED: Begin Work / Decline ────────────────────────
                ServiceRequest.STATUS_ACCEPTED -> {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDeclineDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                        ) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Decline", fontSize = 14.sp)
                        }

                        Button(
                            onClick = { beginningWork = true; onBeginWork() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            enabled = !beginningWork
                        ) {
                            if (beginningWork) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Starting…", fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Begin Work",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── IN PROGRESS: Mark as Complete ─────────────────────────
                ServiceRequest.STATUS_IN_PROGRESS -> {
                    // Status banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2196F3).copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Loop, null, Modifier.size(16.dp), Color(0xFF2196F3))
                            Text(
                                "Work is currently in progress",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }

                    Button(
                        onClick = { completing = true; onMarkComplete() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        enabled = !completing
                    ) {
                        if (completing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Completing…",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Mark as Complete",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ── COMPLETED ─────────────────────────────────────────────
                ServiceRequest.STATUS_COMPLETED -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "Job Completed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                                request.completedAt?.let {
                                    Text(
                                        formatDate(it.toDate().time),
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }

                    if (request.rating != null) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color(0xFFFFC107).copy(alpha = 0.3f)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Member's Rating",
                                fontSize = 11.sp,
                                color = Color(0xFF999999),
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { i ->
                                    Icon(
                                        if (i < request.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (i < request.rating.toInt()) Color(0xFFFFC107) else Color(0xFFCCCCCC)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${request.rating.toInt()} / 5",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF666666)
                                )
                            }
                            if (!request.review.isNullOrBlank()) {
                                Text(
                                    "\"${request.review}\"",
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666),
                                    lineHeight = 18.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    } else {
                        Text(
                            "Awaiting member rating…",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }
            }
        }
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Decline Job?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to decline \"${request.title}\"? " +
                            "It will be returned to the queue for another provider."
                )
            },
            confirmButton = {
                TextButton(onClick = { onDecline(); showDeclineDialog = false }) {
                    Text(
                        "Yes, Decline",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text("Keep Job")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AvatarCircle(name: String, sizeDp: Int) {
    Surface(
        modifier = Modifier.size(sizeDp.dp),
        shape = CircleShape,
        color = BluePrimary.copy(alpha = 0.15f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = BluePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.4f).sp
            )
        }
    }
}

@Composable
private fun IconText(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(13.dp),
            tint = Color(0xFF999999)
        )
        Text(text, fontSize = 12.sp, color = Color(0xFF999999))
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        ServiceRequest.STATUS_PENDING     -> Color(0xFFFF9800) to "PENDING"
        ServiceRequest.STATUS_ACCEPTED    -> BluePrimary       to "ACCEPTED"
        ServiceRequest.STATUS_IN_PROGRESS -> Color(0xFF2196F3) to "IN PROGRESS"
        ServiceRequest.STATUS_COMPLETED   -> Color(0xFF4CAF50) to "COMPLETED"
        ServiceRequest.STATUS_CANCELLED   -> Color(0xFFD32F2F) to "CANCELLED"
        else                              -> Color(0xFF999999) to status.uppercase()
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            label,
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
            null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF666666)
        )
        Text(text, fontSize = 13.sp, color = Color(0xFF666666))
    }
}

private fun formatDate(ts: Long) =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ts))

private fun formatTimestamp(ts: Long): String {
    val d = System.currentTimeMillis() - ts
    return when {
        d < 3_600_000  -> "${d / 60_000}m ago"
        d < 86_400_000 -> "${d / 3_600_000}h ago"
        else           -> "${d / 86_400_000}d ago"
    }
}