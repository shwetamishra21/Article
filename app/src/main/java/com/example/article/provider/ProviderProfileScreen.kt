package com.example.article.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileScreen(
    onLogout: () -> Unit
) {
    var providerName by remember { mutableStateOf("Service Provider") }
    var serviceType by remember { mutableStateOf("Plumbing") }
    var bio by remember { mutableStateOf("Professional service provider with 10+ years of experience.") }
    var isAvailable by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // TODO: Load provider profile from Firebase
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BlueOnPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = BlueOnPrimary
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = BluePrimary.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceLight
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = BluePrimary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = providerName.first().uppercase(),
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }
                    }

                    Text(
                        text = providerName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BluePrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = serviceType,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BluePrimary
                        )
                    }

                    Text(
                        text = bio,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp
                    )
                }
            }

            // Availability Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = BluePrimary.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceLight
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Availability Status",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceLight
                            )
                            Text(
                                text = if (isAvailable) "Available for requests" else "Not available",
                                fontSize = 13.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    Switch(
                        checked = isAvailable,
                        onCheckedChange = {
                            isAvailable = it
                            // TODO: Update availability in Firebase
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = BluePrimary,
                            checkedThumbColor = BlueOnPrimary
                        )
                    )
                }
            }

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Completed", "24", Modifier.weight(1f))
                StatCard("In Progress", "3", Modifier.weight(1f))
                StatCard("Rating", "4.8", Modifier.weight(1f))
            }

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Logout", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(providerName) }
        var editBio by remember { mutableStateOf(bio) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        providerName = editName
                        bio = editBio
                        showEditDialog = false
                        // TODO: Update in Firebase
                    }
                ) {
                    Text("Save", color = BluePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Logout Confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(16.dp),
            spotColor = BluePrimary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}