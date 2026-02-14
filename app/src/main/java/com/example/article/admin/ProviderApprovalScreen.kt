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

data class ServiceProvider(
    val id: String,
    val name: String,
    val category: String,
    val skills: List<String>,
    val rating: Float?,
    val reviewCount: Int,
    val status: ProviderStatus
)

enum class ProviderStatus {
    PENDING, APPROVED, REJECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderApprovalScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pending", "Approved", "Rejected")

    var showApproveDialog by remember { mutableStateOf<ServiceProvider?>(null) }
    var showRejectDialog by remember { mutableStateOf<ServiceProvider?>(null) }
    var showRemoveDialog by remember { mutableStateOf<ServiceProvider?>(null) }

    // Sample data
    val providers = remember {
        listOf(
            ServiceProvider(
                "1", "Mike's Plumbing", "Plumber",
                listOf("Pipe repair", "Leak fixing", "Installation"),
                4.8f, 156, ProviderStatus.APPROVED
            ),
            ServiceProvider(
                "2", "Spark Electric", "Electrician",
                listOf("Wiring", "Repairs", "Installation"),
                4.9f, 203, ProviderStatus.APPROVED
            ),
            ServiceProvider(
                "3", "Quick Fix AC", "AC Technician",
                listOf("AC Repair", "Installation", "Maintenance"),
                null, 0, ProviderStatus.PENDING
            ),
            ServiceProvider(
                "4", "Home Painters", "Painter",
                listOf("Interior", "Exterior", "Wall design"),
                null, 0, ProviderStatus.PENDING
            ),
            ServiceProvider(
                "5", "Budget Repairs", "Handyman",
                listOf("General repairs"),
                3.2f, 12, ProviderStatus.REJECTED
            )
        )
    }

    val filteredProviders = when (selectedTab) {
        0 -> providers.filter { it.status == ProviderStatus.PENDING }
        1 -> providers.filter { it.status == ProviderStatus.APPROVED }
        2 -> providers.filter { it.status == ProviderStatus.REJECTED }
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
                // TODO: Approve provider logic
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
                // TODO: Reject provider logic
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
                // TODO: Remove provider logic
                showRemoveDialog = null
            }
        )
    }
}

@Composable
fun ProviderCard(
    provider: ServiceProvider,
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
                val icon = when (provider.category) {
                    "Plumber" -> Icons.Default.Plumbing
                    "Electrician" -> Icons.Default.ElectricalServices
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
                        ProviderStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                        ProviderStatus.APPROVED -> MaterialTheme.colorScheme.tertiaryContainer
                        ProviderStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = provider.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (provider.status) {
                            ProviderStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
                            ProviderStatus.APPROVED -> MaterialTheme.colorScheme.onTertiaryContainer
                            ProviderStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            // Skills
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Skills: ${provider.skills.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Action Buttons
            when (provider.status) {
                ProviderStatus.PENDING -> {
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
                ProviderStatus.APPROVED -> {
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
                ProviderStatus.REJECTED -> {
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