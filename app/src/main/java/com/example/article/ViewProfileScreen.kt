package com.example.article

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.article.Repository.ProfileViewModel
import com.example.article.Repository.ProfileUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewProfileScreen(
    navController: NavController,
    userId: String,
    userRole: String,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Provider-specific data
    var serviceType by remember { mutableStateOf("") }
    var isAvailable by remember { mutableStateOf(false) }
    var completedJobs by remember { mutableStateOf(0) }
    var activeRequests by remember { mutableStateOf(0) }
    var rating by remember { mutableStateOf(5.0) }

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)

        // Load provider-specific data if user is a provider
        if (userRole == "service_provider") {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // Get service type and availability
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                serviceType = userDoc.getString("serviceType") ?: "Service Provider"
                isAvailable = userDoc.getBoolean("isAvailable") ?: false

                // Get completed jobs count
                val completedSnapshot = firestore.collection("service_requests")
                    .whereEqualTo("providerId", userId)
                    .whereEqualTo("status", "completed")
                    .get()
                    .await()

                completedJobs = completedSnapshot.size()

                // Get active requests count
                val activeSnapshot = firestore.collection("service_requests")
                    .whereEqualTo("providerId", userId)
                    .whereIn("status", listOf("accepted", "in_progress"))
                    .get()
                    .await()

                activeRequests = activeSnapshot.size()

                // Get rating (default 5.0 for now, can be calculated from reviews later)
                rating = userDoc.getDouble("rating") ?: 5.0

            } catch (e: Exception) {
                // Silently fail, use default values
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
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
                            colors = listOf(
                                Color(0xFF42A5F5),
                                Color(0xFF4DD0E1)
                            )
                        )
                    )
                    .shadow(
                        elevation = 6.dp,
                        spotColor = Color(0xFF42A5F5).copy(alpha = 0.4f)
                    )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            ProfileUiState.Loading -> {
                LoadingState(message = "Loading profile...")
            }

            is ProfileUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadUserProfile(userId) }
                )
            }

            is ProfileUiState.Success -> {
                val profile = state.profile

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5))
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp)
                ) {
                    // Profile Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Profile Image
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .border(
                                        width = 4.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color(0xFF42A5F5),
                                                Color(0xFF4DD0E1)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .clip(CircleShape)
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
                            }

                            // Name
                            Text(
                                text = profile.name.ifEmpty { "User" },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1a1a1a)
                            )

                            // Bio
                            if (profile.bio.isNotEmpty()) {
                                Text(
                                    text = profile.bio,
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }

                            // Neighborhood
                            if (profile.neighbourhood.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF42A5F5)
                                    )
                                    Text(
                                        text = profile.neighbourhood,
                                        fontSize = 14.sp,
                                        color = Color(0xFF42A5F5),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Role Badge (Service Provider shows service type)
                            Surface(
                                color = when (userRole) {
                                    "service_provider" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    "admin" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                    else -> Color(0xFF42A5F5).copy(alpha = 0.15f)
                                },
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
                                        when (userRole) {
                                            "service_provider" -> Icons.Default.Build
                                            "admin" -> Icons.Default.AdminPanelSettings
                                            else -> Icons.Default.Person
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = when (userRole) {
                                            "service_provider" -> Color(0xFF4CAF50)
                                            "admin" -> Color(0xFFFF9800)
                                            else -> Color(0xFF42A5F5)
                                        }
                                    )
                                    Text(
                                        text = when (userRole) {
                                            "service_provider" -> serviceType.ifEmpty { "Service Provider" }
                                            "admin" -> "Admin"
                                            else -> "Member"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (userRole) {
                                            "service_provider" -> Color(0xFF4CAF50)
                                            "admin" -> Color(0xFFFF9800)
                                            else -> Color(0xFF42A5F5)
                                        }
                                    )
                                }
                            }

                            // Send Message Button (don't show for own profile)
                            if (currentUserId != null && currentUserId != userId) {
                                Button(
                                    onClick = {
                                        // Create chat ID from both user IDs (sorted for consistency)
                                        val chatId = if (currentUserId < userId) {
                                            "${currentUserId}_${userId}"
                                        } else {
                                            "${userId}_${currentUserId}"
                                        }
                                        navController.navigate("chat/$chatId/${profile.name}")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF42A5F5)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Send Message",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Provider-specific sections
                    if (userRole == "service_provider") {
                        Spacer(Modifier.height(16.dp))

                        // Availability Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Availability Status",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1a1a1a)
                                    )
                                    Text(
                                        text = if (isAvailable) "Available for requests" else "Not available",
                                        fontSize = 13.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
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
                            ProviderStatCard(
                                label = "Completed",
                                value = completedJobs.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            ProviderStatCard(
                                label = "Active",
                                value = activeRequests.toString(),
                                icon = Icons.Default.Pending,
                                color = Color(0xFF42A5F5),
                                modifier = Modifier.weight(1f)
                            )
                            ProviderStatCard(
                                label = "Rating",
                                value = String.format("%.1f", rating),
                                icon = Icons.Default.Star,
                                color = Color(0xFFFFA000),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            spotColor = color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
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