package com.example.article

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    username: String,
    onUsernameChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var editableUsername by remember { mutableStateOf(username) }
    var bio by remember { mutableStateOf("Creative mind passionate about design and technology. Always learning, always growing. 🚀") }
    var showEditDialog by remember { mutableStateOf(false) }
    var isEditingBio by remember { mutableStateOf(false) }

    // Animation for profile avatar
    val infiniteTransition = rememberInfiniteTransition(label = "profile_animation")
    val profileScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "profile_scale"
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            LavenderMist,
            SoftLilac.copy(alpha = 0.3f),
            RoyalViolet.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BrightWhite)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile avatar with animation
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = RoyalViolet
                        ),
                        modifier = Modifier
                            .size(120.dp)
                            .scale(profileScale),
                        elevation = CardDefaults.cardElevation(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = editableUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            editableUsername.split("@")[0].replaceFirstChar { it.uppercase() },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepPlum
                        )
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit username",
                                tint = RoyalViolet,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        editableUsername,
                        fontSize = 14.sp,
                        color = SteelGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bio section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SoftLilac.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Bio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepPlum
                                )
                                IconButton(
                                    onClick = { isEditingBio = !isEditingBio },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        if (isEditingBio) Icons.Filled.Check else Icons.Filled.Edit,
                                        contentDescription = if (isEditingBio) "Save bio" else "Edit bio",
                                        tint = RoyalViolet,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (isEditingBio) {
                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RoyalViolet,
                                        cursorColor = RoyalViolet
                                    ),
                                    maxLines = 3
                                )
                            } else {
                                Text(
                                    bio,
                                    fontSize = 12.sp,
                                    color = SteelGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Posts",
                    value = "42",
                    icon = Icons.Filled.Article,
                    color = RoyalViolet,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Followers",
                    value = "1.2K",
                    icon = Icons.Filled.Group,
                    color = DeepPlum,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Likes",
                    value = "5.6K",
                    icon = Icons.Filled.Favorite,
                    color = Color.Red.copy(0.8f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings Section
            SettingsCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Logout Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
            ) {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red,
                        containerColor = Color.Transparent
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.Red, Color.Red.copy(0.7f))))
                ) {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = "Logout",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sign Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Edit Username Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Username", fontWeight = FontWeight.Bold, color = DeepPlum) },
            text = {
                OutlinedTextField(
                    value = editableUsername,
                    onValueChange = { editableUsername = it },
                    label = { Text("Username") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalViolet,
                        focusedLabelColor = RoyalViolet,
                        cursorColor = RoyalViolet
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUsernameChange(editableUsername)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = SteelGray)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrightWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPlum
            )
            Text(
                title,
                fontSize = 12.sp,
                color = SteelGray
            )
        }
    }
}

@Composable
fun SettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrightWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPlum
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = "Manage your notification preferences"
            )

            SettingsItem(
                icon = Icons.Filled.Security,
                title = "Privacy & Security",
                subtitle = "Control your privacy settings"
            )

            SettingsItem(
                icon = Icons.Filled.Palette,
                title = "Theme",
                subtitle = "Customize app appearance"
            )

            SettingsItem(
                icon = Icons.Filled.Help,
                title = "Help & Support",
                subtitle = "Get help and contact support"
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = RoyalViolet,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DeepPlum
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = SteelGray
            )
        }

        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "Navigate",
            tint = SteelGray,
            modifier = Modifier.size(20.dp)
        )
    }
}