package com.example.article

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.article.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
// Data holder loaded from Firestore
// ─────────────────────────────────────────────────────────────────────────────
private data class ProviderPublicProfile(
    val name: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val serviceType: String = "",
    val skills: List<String> = emptyList(),
    val averageRating: Float = 0f,
    val ratingCount: Int = 0,
    val completedJobs: Int = 0,
    val isAvailable: Boolean = true
)

private sealed class SheetState {
    object Loading : SheetState()
    data class Success(val profile: ProviderPublicProfile) : SheetState()
    data class Error(val message: String) : SheetState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet entry point — called from RequestsScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProviderProfileSheet(
    providerId: String,
    onDismiss: () -> Unit
) {
    var sheetState by remember { mutableStateOf<SheetState>(SheetState.Loading) }

    // Load the provider's public profile from Firestore once
    LaunchedEffect(providerId) {
        sheetState = SheetState.Loading
        try {
            val firestore = FirebaseFirestore.getInstance()

            val userDoc = firestore.collection("users")
                .document(providerId)
                .get()
                .await()

            // Completed jobs count
            val completedSnap = firestore.collection("service_requests")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "completed")
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val skills = (userDoc.get("skills") as? List<String>) ?: emptyList()

            sheetState = SheetState.Success(
                ProviderPublicProfile(
                    name = userDoc.getString("name") ?: "",
                    photoUrl = userDoc.getString("photoUrl") ?: "",
                    bio = userDoc.getString("bio") ?: "",
                    serviceType = userDoc.getString("serviceType") ?: "",
                    skills = skills,
                    averageRating = (userDoc.getDouble("averageRating") ?: 0.0).toFloat(),
                    ratingCount = (userDoc.getLong("ratingCount") ?: 0L).toInt(),
                    completedJobs = completedSnap.size(),
                    isAvailable = userDoc.getBoolean("isAvailable") ?: true
                )
            )
        } catch (e: Exception) {
            sheetState = SheetState.Error(e.message ?: "Failed to load provider profile")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(
                        Color(0xFFDDDDDD),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        when (val state = sheetState) {

            SheetState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            }

            is SheetState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFD32F2F)
                    )
                    Text(
                        "Couldn't load provider profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        state.message,
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }

            is SheetState.Success -> {
                val profile = state.profile

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    // ── Header gradient band ────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(BluePrimary, BlueSecondary)
                                )
                            )
                    )

                    // ── Avatar overlapping the gradient ─────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Negative offset so avatar sits half on gradient, half below
                        Box(modifier = Modifier.offset(y = (-44).dp)) {
                            if (profile.photoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = profile.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(88.dp),
                                    shape = CircleShape,
                                    color = BluePrimary.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        3.dp, Color.White
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = profile.name.firstOrNull()?.uppercase() ?: "P",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }
                                }
                            }

                            // Availability dot
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp),
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .background(
                                            if (profile.isAvailable)
                                                Color(0xFF4CAF50)
                                            else
                                                Color(0xFFBBBBBB),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }

                    // ── Name + trade + availability label ───────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 52.dp)        // compensates for avatar offset
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profile.name.ifEmpty { "Provider" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1a1a1a)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Service type badge
                            Surface(
                                color = BluePrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = BluePrimary
                                    )
                                    Text(
                                        profile.serviceType,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                }
                            }

                            // Availability badge
                            Surface(
                                color = if (profile.isAvailable)
                                    Color(0xFF4CAF50).copy(alpha = 0.12f)
                                else
                                    Color(0xFF999999).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (profile.isAvailable) "Available" else "Unavailable",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (profile.isAvailable)
                                        Color(0xFF4CAF50)
                                    else
                                        Color(0xFF999999)
                                )
                            }
                        }

                        // Star rating
                        if (profile.averageRating > 0f) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { i ->
                                    Icon(
                                        imageVector = if (i < profile.averageRating.toInt())
                                            Icons.Default.Star
                                        else
                                            Icons.Default.StarBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (i < profile.averageRating.toInt())
                                            Color(0xFFFFC107)
                                        else
                                            Color(0xFFCCCCCC)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${"%.1f".format(profile.averageRating)} · ${profile.ratingCount} reviews",
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Bio
                        if (profile.bio.isNotEmpty()) {
                            Text(
                                text = profile.bio,
                                fontSize = 13.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Stats row ───────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MiniStatCard(
                            label = "Jobs Done",
                            value = profile.completedJobs.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconTint = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatCard(
                            label = "Rating",
                            value = if (profile.averageRating > 0f)
                                "%.1f".format(profile.averageRating)
                            else "—",
                            icon = Icons.Default.Star,
                            iconTint = Color(0xFFFFC107),
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatCard(
                            label = "Reviews",
                            value = profile.ratingCount.toString(),
                            icon = Icons.Default.RateReview,
                            iconTint = BluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ── Skills ──────────────────────────────────────────────
                    if (profile.skills.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Specializations",
                            modifier = Modifier.padding(horizontal = 24.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1a1a1a)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Wrap skills in rows of 2
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profile.skills.chunked(2).forEach { rowSkills ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSkills.forEach { skill ->
                                        Surface(
                                            color = BlueSecondary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 8.dp
                                                ),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = BlueSecondary
                                                )
                                                Text(
                                                    text = skill,
                                                    fontSize = 12.sp,
                                                    color = BlueSecondary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    // Fill empty slot if odd number of skills in row
                                    if (rowSkills.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Close button ────────────────────────────────────────
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small stat card used inside the sheet
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1a1a1a)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )
        }
    }
}