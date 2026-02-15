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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.ProviderApprovalViewModel
import com.example.article.Repository.AdminProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProviderApprovalViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pending", "Approved", "Rejected")

    var showApproveDialog by remember { mutableStateOf<AdminProvider?>(null) }
    var showRejectDialog by remember { mutableStateOf<AdminProvider?>(null) }
    var showRemoveDialog by remember { mutableStateOf<AdminProvider?>(null) }

    // Load providers on screen open
    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.loadProviders()
        }
    }

    val filteredProviders = when (selectedTab) {
        0 -> providers.filter { it.status == "pending" }
        1 -> providers.filter { it.status == "approved" }
        2 -> providers.filter { it.status == "rejected" }
        else -> providers
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Approval") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Loading State
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Error State
            error?.let {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error: $it",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { scope.launch { viewModel.loadProviders() } }) {
                            Text("Retry")
                        }
                    }
                }
                return@Column
            }

            // Providers List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProviders) { provider ->
                    ProviderCard(
                        provider = provider,
                        onApprove = { showApproveDialog = provider },
                        onReject = { showRejectDialog = provider },
                        onRemove = { showRemoveDialog = provider }
                    )
                }

                if (filteredProviders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${tabs[selectedTab].lowercase()} providers",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Approve Dialog
    showApproveDialog?.let { provider ->
        ConfirmDialog(
            icon = Icons.Default.CheckCircle,
            iconTint = MaterialTheme.colorScheme.tertiary,
            title = "Approve Provider?",
            message = "Do you want to approve ${provider.name} as a service provider?",
            confirmText = "Approve",
            onDismiss = { showApproveDialog = null },
            onConfirm = {
                scope.launch {
                    viewModel.approveProvider(provider.id)
                }
                showApproveDialog = null
            }
        )
    }

    // Reject Dialog
    showRejectDialog?.let { provider ->
        ConfirmDialog(
            icon = Icons.Default.Cancel,
            iconTint = MaterialTheme.colorScheme.error,
            title = "Reject Provider?",
            message = "Do you want to reject ${provider.name}'s application?",
            confirmText = "Reject",
            confirmColor = MaterialTheme.colorScheme.error,
            onDismiss = { showRejectDialog = null },
            onConfirm = {
                scope.launch {
                    viewModel.rejectProvider(provider.id)
                }
                showRejectDialog = null
            }
        )
    }

    // Remove Dialog
    showRemoveDialog?.let { provider ->
        ConfirmDialog(
            icon = Icons.Default.Warning,
            iconTint = MaterialTheme.colorScheme.error,
            title = "Remove Provider?",
            message = "Are you sure you want to remove ${provider.name}? This action cannot be undone.",
            confirmText = "Remove",
            confirmColor = MaterialTheme.colorScheme.error,
            onDismiss = { showRemoveDialog = null },
            onConfirm = {
                scope.launch {
                    viewModel.removeProvider(provider.id)
                }
                showRemoveDialog = null
            }
        )
    }
}

@Composable
fun ProviderCard(
    provider: AdminProvider,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon based on category
                val icon = when (provider.category.lowercase()) {
                    "plumber" -> Icons.Default.Plumbing
                    "electrician" -> Icons.Default.ElectricalServices
                    else -> Icons.Default.Build
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = provider.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Rating (if approved)
                    if (provider.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${provider.rating} • ${provider.reviewCount} reviews",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    color = when (provider.status) {
                        "pending" -> MaterialTheme.colorScheme.secondaryContainer
                        "approved" -> MaterialTheme.colorScheme.tertiaryContainer
                        "rejected" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = provider.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (provider.status) {
                            "pending" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "approved" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "rejected" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Skills
            if (provider.skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Skills: ${provider.skills.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Action Buttons
            when (provider.status) {
                "pending" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject")
                        }
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve")
                        }
                    }
                }
                "approved" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove Provider")
                    }
                }
                "rejected" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reconsider")
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    confirmText: String,
    confirmColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(icon, contentDescription = null, tint = iconTint)
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}