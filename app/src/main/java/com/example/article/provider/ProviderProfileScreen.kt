package com.example.article.provider

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.article.Repository.ProfileViewModel
import com.example.article.Repository.ProfileUiState
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editServiceType by remember { mutableStateOf("Plumber") }
    var isAvailable by remember { mutableStateOf(true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var expandedServiceType by remember { mutableStateOf(false) }

    val serviceTypes = listOf(
        "Plumber", "Electrician", "Cleaner", "Carpenter", "Painter",
        "Gardener", "AC Repair", "Appliance Repair", "Pest Control",
        "Locksmith", "Handyman", "Mason", "Welder", "Tailor",
        "Beautician", "Tutor", "Chef/Cook", "Driver", "Security Guard",
        "Moving & Packing", "Interior Designer", "Solar Panel Installer",
        "Water Tank Cleaner", "Car Wash", "Other"
    )

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadProfileImage(it, context) {
                selectedImageUri = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BlueOnPrimary
                    )
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                Icons.Default.Logout,
                                "Logout",
                                tint = BlueOnPrimary
                            )
                        }
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
        when (val state = uiState) {
            ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(BackgroundLight),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.padding(24.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceLight,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color(0xFFD32F2F)
                            )
                            Text(
                                "Error loading profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                state.message,
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadProfile() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BluePrimary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            is ProfileUiState.Success -> {
                val profile = state.profile

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(BackgroundLight)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp)
                ) {
                    // Profile Header Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceLight,
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Profile Image with Gradient Border
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .border(
                                        width = 4.dp,
                                        brush = Brush.linearGradient(
                                            listOf(BluePrimary, BlueSecondary)
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePicker.launch("image/*") }
                                    .background(Color(0xFFF0F0F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profile.photoUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(profile.photoUrl),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(52.dp),
                                        tint = Color(0xFF999999)
                                    )
                                }

                                if (isUpdating) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Camera icon overlay
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(32.dp),
                                    shape = CircleShape,
                                    color = BluePrimary,
                                    shadowElevation = 4.dp
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Change photo",
                                        modifier = Modifier.padding(6.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            // Name & Service Type
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Name", fontSize = 14.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )

                                ExposedDropdownMenuBox(
                                    expanded = expandedServiceType,
                                    onExpandedChange = { expandedServiceType = it }
                                ) {
                                    OutlinedTextField(
                                        value = editServiceType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Service Type", fontSize = 14.sp) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expandedServiceType
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BluePrimary,
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expandedServiceType,
                                        onDismissRequest = { expandedServiceType = false },
                                        modifier = Modifier.heightIn(max = 300.dp)
                                    ) {
                                        serviceTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type, fontSize = 13.sp) },
                                                onClick = {
                                                    editServiceType = type
                                                    expandedServiceType = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = editBio,
                                    onValueChange = { editBio = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Bio", fontSize = 14.sp) },
                                    placeholder = { Text("Tell clients about your expertise...") },
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                            } else {
                                Text(
                                    text = profile.name.ifEmpty { "Set your name" },
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceLight
                                )

                                // Service Type Badge
                                Surface(
                                    color = BluePrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 18.dp,
                                            vertical = 10.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = BluePrimary
                                        )
                                        Text(
                                            text = editServiceType,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }
                                }

                                Text(
                                    text = profile.bio.ifEmpty { "Add a bio to tell clients about your expertise" },
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }

                            // Edit/Save Buttons
                            if (isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isEditing = false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isUpdating
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateProfile(editName, editBio, "") {
                                                isEditing = false
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isUpdating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BluePrimary
                                        )
                                    ) {
                                        if (isUpdating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.5.dp,
                                                color = Color.White
                                            )
                                        } else {
                                            Icon(Icons.Default.Check, null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Save", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        editName = profile.name
                                        editBio = profile.bio
                                        isEditing = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BluePrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Edit, null)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Edit Profile", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Availability Toggle Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
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
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = BluePrimary,
                                    checkedThumbColor = BlueOnPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Stats Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Completed",
                            value = "0",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Active",
                            value = "0",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Rating",
                            value = "5.0",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Logout Confirmation Dialog
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
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            spotColor = BluePrimary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BluePrimary
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )
        }
    }
}