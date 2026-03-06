package com.example.article.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.AdminNeighbourhoodViewModel
import com.example.article.Repository.JoinRequestViewModel
import com.example.article.Repository.MemberManagementViewModel
import com.example.article.Repository.ProviderApprovalViewModel
import com.example.article.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToMembers: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onNavigateToModeration: () -> Unit,
    onNavigateToJoinRequests: () -> Unit,
    memberViewModel: MemberManagementViewModel = viewModel(),
    providerViewModel: ProviderApprovalViewModel = viewModel(),
    joinRequestViewModel: JoinRequestViewModel = viewModel(),
    neighbourhoodViewModel: AdminNeighbourhoodViewModel = viewModel()
) {
    val members by memberViewModel.members.collectAsState()
    val providers by providerViewModel.providers.collectAsState()
    val joinRequests by joinRequestViewModel.requests.collectAsState()
    val neighbourhood by neighbourhoodViewModel.neighbourhood.collectAsState()
    val neighLoading by neighbourhoodViewModel.isLoading.collectAsState()
    val neighMessage by neighbourhoodViewModel.message.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            memberViewModel.loadMembers()
            providerViewModel.loadProviders()
            joinRequestViewModel.loadForAdmin()
            neighbourhoodViewModel.loadAdminNeighbourhood()
        }
    }

    // Snackbar + auto-close dialog on success
    LaunchedEffect(neighMessage) {
        neighMessage?.let {
            snackbarHostState.showSnackbar(it)
            neighbourhoodViewModel.clearMessage()
            if (it.contains("success", ignoreCase = true)) {
                showCreateDialog = false
            }
        }
    }

    val hasNeighbourhood = neighbourhood != null && !neighLoading
    val activeProviders = providers.count { it.status == "approved" }
    val pendingProviders = providers.count { it.status == "pending" }
    val pendingJoinRequests = joinRequests.size

    if (showCreateDialog) {
        CreateNeighbourhoodDialog(
            isLoading = neighLoading,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                neighbourhoodViewModel.createNeighbourhood(name, description)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Admin Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BlueOnPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(Brush.linearGradient(colors = listOf(BluePrimary, BlueSecondary)))
                    .shadow(elevation = 6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        }
    ) { paddingValues ->

        // Full-screen loader only on very first load
        if (neighLoading && neighbourhood == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BluePrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── NEIGHBOURHOOD BANNER / SETUP CARD ─────────────────────
            item {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    if (!hasNeighbourhood) {
                        SetupNeighbourhoodCard(onClick = { showCreateDialog = true })
                    } else {
                        NeighbourhoodBanner(
                            name = neighbourhood!!.name,
                            memberCount = neighbourhood!!.memberCount,
                            providerCount = neighbourhood!!.providerCount
                        )
                    }
                }
            }

            // ── STATS ─────────────────────────────────────────────────
            item {
                Text(
                    "Neighbourhood Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurfaceLight,
                    modifier = Modifier.alpha(if (hasNeighbourhood) 1f else 0.4f)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().alpha(if (hasNeighbourhood) 1f else 0.4f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Person,
                        title = "Members",
                        value = if (hasNeighbourhood) members.size.toString() else "—",
                        backgroundColor = BluePrimary.copy(alpha = 0.15f)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Build,
                        title = "Providers",
                        value = if (hasNeighbourhood) activeProviders.toString() else "—",
                        backgroundColor = BlueSecondary.copy(alpha = 0.15f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().alpha(if (hasNeighbourhood) 1f else 0.4f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.HourglassEmpty,
                        title = "Pending",
                        value = if (hasNeighbourhood) pendingProviders.toString() else "—",
                        backgroundColor = Color(0xFFFF9800).copy(alpha = 0.15f)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Group,
                        title = "Total",
                        value = if (hasNeighbourhood) (members.size + activeProviders).toString() else "—",
                        backgroundColor = BlueTertiary.copy(alpha = 0.15f)
                    )
                }
            }

            // ── QUICK ACTIONS ─────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurfaceLight,
                    modifier = Modifier.alpha(if (hasNeighbourhood) 1f else 0.4f)
                )
            }

            item {
                AdminActionCard(
                    icon = Icons.Default.Group,
                    title = "Member Management",
                    description = "Add/remove members from neighbourhood",
                    badge = if (hasNeighbourhood) members.size.toString() else null,
                    enabled = hasNeighbourhood,
                    onClick = onNavigateToMembers
                )
            }

            item {
                AdminActionCard(
                    icon = Icons.Default.Verified,
                    title = "Provider Approval",
                    description = "Approve/remove service providers",
                    badge = if (hasNeighbourhood && pendingProviders > 0) "$pendingProviders pending" else null,
                    enabled = hasNeighbourhood,
                    onClick = onNavigateToProviders
                )
            }

            item {
                AdminActionCard(
                    icon = Icons.Default.Campaign,
                    title = "Announcements",
                    description = "Create pinned announcements",
                    enabled = hasNeighbourhood,
                    onClick = onNavigateToAnnouncements
                )
            }

            item {
                AdminActionCard(
                    icon = Icons.Default.Shield,
                    title = "Content Moderation",
                    description = "Review/remove posts",
                    enabled = hasNeighbourhood,
                    onClick = onNavigateToModeration
                )
            }

            item {
                AdminActionCard(
                    icon = Icons.Default.PersonAdd,
                    title = "Join Requests",
                    description = "Approve or reject neighbourhood join requests",
                    badge = if (hasNeighbourhood && pendingJoinRequests > 0) "$pendingJoinRequests pending" else null,
                    enabled = hasNeighbourhood,
                    onClick = onNavigateToJoinRequests
                )
            }
        }
    }
}

// ── CREATE NEIGHBOURHOOD DIALOG ───────────────────────────────────────────

@Composable
private fun CreateNeighbourhoodDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceLight,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(BluePrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Home, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Create Neighbourhood", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text("This will be your community space", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Neighbourhood Name *") },
                    placeholder = { Text("e.g. Green Residency, Sunset Apartments") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    singleLine = true,
                    enabled = !isLoading
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Brief description of your neighbourhood") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    minLines = 2,
                    maxLines = 3,
                    enabled = !isLoading
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            if (name.isBlank()) nameError = true
                            else onCreate(name.trim(), description.trim())
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Create", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── SETUP CARD (shown when admin has no neighbourhood) ────────────────────

@Composable
private fun SetupNeighbourhoodCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        color = BluePrimary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No Neighbourhood Yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Create your neighbourhood so members can find and join your community.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Default.Add, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Neighbourhood", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── NEIGHBOURHOOD BANNER (shown when neighbourhood exists) ────────────────

@Composable
private fun NeighbourhoodBanner(name: String, memberCount: Int, providerCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        color = BluePrimary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(BluePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                Text(text = "$memberCount members · $providerCount providers", fontSize = 12.sp, color = Color(0xFF555555))
            }
            Surface(color = Color(0xFF4CAF50).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text("Active", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
            }
        }
    }
}

// ── STAT CARD ─────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(backgroundColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = BluePrimary)
            }
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
            Text(text = title, fontSize = 12.sp, color = Color(0xFF666666))
        }
    }
}

// ── ADMIN ACTION CARD ─────────────────────────────────────────────────────

@Composable
fun AdminActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    badge: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = if (enabled) 4.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = BluePrimary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(BluePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                    badge?.let {
                        Surface(color = Color(0xFFFF9800).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(text = it, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        }
                    }
                }
                Text(text = description, fontSize = 13.sp, color = Color(0xFF666666))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(24.dp))
        }
    }
}