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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.article.Repository.AdminProvider
import com.example.article.Repository.ProviderApprovalViewModel
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProviderApprovalViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pending", "Approved", "Rejected")

    var showApproveDialog by remember { mutableStateOf<AdminProvider?>(null) }
    var showRejectDialog by remember { mutableStateOf<AdminProvider?>(null) }
    var showRemoveDialog by remember { mutableStateOf<AdminProvider?>(null) }

    LaunchedEffect(Unit) { viewModel.loadProviders() }
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }

    val filteredProviders = when (selectedTab) {
        0 -> providers.filter { it.status == "pending" }
        1 -> providers.filter { it.status == "approved" }
        2 -> providers.filter { it.status == "rejected" }
        else -> providers
    }
    val pendingCount = providers.count { it.status == "pending" }

    showApproveDialog?.let { provider ->
        AlertDialog(
            onDismissRequest = { showApproveDialog = null },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Approve Provider?") },
            text = { Text("${provider.name} will be approved and can start receiving service requests.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.approveProvider(provider.id); showApproveDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Approve") }
            },
            dismissButton = { TextButton(onClick = { showApproveDialog = null }) { Text("Cancel") } }
        )
    }

    showRejectDialog?.let { provider ->
        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            icon = { Icon(Icons.Default.Cancel, null, tint = Color(0xFFCC4444)) },
            title = { Text("Reject Provider?") },
            text = { Text("${provider.name}'s application will be rejected.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.rejectProvider(provider.id); showRejectDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4444))
                ) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { showRejectDialog = null }) { Text("Cancel") } }
        )
    }

    showRemoveDialog?.let { provider ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFCC4444)) },
            title = { Text("Remove Provider?") },
            text = { Text("${provider.name} will be removed from the neighbourhood. They can reapply.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.removeProvider(provider.id); showRemoveDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4444))
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Provider Approval", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text(
                            "${providers.size} total${if (pendingCount > 0) " · $pendingCount pending" else ""}",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadProviders() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(Brush.linearGradient(colors = listOf(BluePrimary, BlueSecondary)))
                    .shadow(elevation = 6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BluePrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = BluePrimary,
                        unselectedContentColor = Color(0xFF999999),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal)
                                if (index == 0 && pendingCount > 0) {
                                    Surface(color = Color(0xFFFF9800), shape = CircleShape) {
                                        Text("$pendingCount", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            when {
                loading && providers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BluePrimary)
                    }
                }
                error != null && providers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = Color(0xFFCC4444))
                            Text(error ?: "Error", color = Color(0xFFCC4444), fontSize = 14.sp)
                            Button(
                                onClick = { viewModel.loadProviders() },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) { Text("Retry") }
                        }
                    }
                }
                filteredProviders.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                when (selectedTab) {
                                    0 -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Build
                                },
                                null,
                                modifier = Modifier.size(56.dp),
                                tint = if (selectedTab == 0) Color(0xFF4CAF50).copy(alpha = 0.6f) else BluePrimary.copy(alpha = 0.4f)
                            )
                            Text("No ${tabs[selectedTab].lowercase()} providers", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProviders, key = { it.id }) { provider ->
                            ProviderCard(
                                provider = provider,
                                onApprove = { showApproveDialog = provider },
                                onReject = { showRejectDialog = provider },
                                onRemove = { showRemoveDialog = provider }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AdminProvider,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(BluePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (provider.category.lowercase()) {
                        "plumber", "plumbing" -> Icons.Default.Plumbing
                        "electrician", "electrical" -> Icons.Default.ElectricalServices
                        else -> Icons.Default.Build
                    }
                    Icon(icon, null, tint = BluePrimary, modifier = Modifier.size(26.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(provider.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                    Text(provider.category.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = Color(0xFF666666))
                    if (provider.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(13.dp), tint = Color(0xFFFFC107))
                            Text("${provider.rating} · ${provider.reviewCount} reviews", fontSize = 11.sp, color = Color(0xFF888888))
                        }
                    }
                }

                val (badgeColor, badgeTextColor) = when (provider.status) {
                    "approved" -> Color(0xFF4CAF50).copy(alpha = 0.12f) to Color(0xFF4CAF50)
                    "rejected" -> Color(0xFFCC4444).copy(alpha = 0.12f) to Color(0xFFCC4444)
                    else -> Color(0xFFFF9800).copy(alpha = 0.12f) to Color(0xFFFF9800)
                }
                Surface(color = badgeColor, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        provider.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = badgeTextColor
                    )
                }
            }

            if (provider.skills.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    provider.skills.take(3).forEach { skill ->
                        Surface(color = BluePrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp)) {
                            Text(skill, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = BluePrimary)
                        }
                    }
                    if (provider.skills.size > 3) {
                        Text("+${provider.skills.size - 3} more", fontSize = 11.sp, color = Color(0xFF999999),
                            modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            }

            when (provider.status) {
                "pending" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onReject, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC4444)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCC4444).copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = onApprove, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Approve", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                "approved" -> {
                    OutlinedButton(
                        onClick = onRemove, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCC4444).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Remove Provider", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                "rejected" -> {
                    Button(
                        onClick = onApprove, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Reconsider", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}