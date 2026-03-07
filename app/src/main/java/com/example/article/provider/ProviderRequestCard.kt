package com.example.article.provider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.Repository.ServiceRequest
import com.example.article.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProviderRequestCard(
    request: ServiceRequest,
    onAccept: () -> Unit,
    onComplete: () -> Unit,
    onStartWork: () -> Unit = {},
    onDecline: () -> Unit = {},
    // Optional: pass the provider's neighbourhood name to show a
    // "same neighbourhood" confirmation tag on the card.
    providerNeighbourhoodName: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f)

    // True when the request comes from a member in the provider's own neighbourhood
    val isSameNeighbourhood = providerNeighbourhoodName != null &&
            request.memberNeighborhood.isNotBlank() &&
            request.memberNeighborhood.equals(providerNeighbourhoodName, ignoreCase = true)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (request.status == ServiceRequest.STATUS_PENDING) 8.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = when (request.status) {
                    ServiceRequest.STATUS_PENDING    -> Color(0xFFFF9800).copy(alpha = 0.4f)
                    ServiceRequest.STATUS_ACCEPTED   -> BluePrimary.copy(alpha = 0.3f)
                    ServiceRequest.STATUS_IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.3f)
                    ServiceRequest.STATUS_COMPLETED  -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else                             -> Color.Black.copy(alpha = 0.1f)
                }
            ),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceLight,
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // ── Coloured left accent bar ──────────────────────────────────
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        when (request.status) {
                            ServiceRequest.STATUS_PENDING     -> Color(0xFFFF9800)
                            ServiceRequest.STATUS_ACCEPTED    -> BluePrimary
                            ServiceRequest.STATUS_IN_PROGRESS -> Color(0xFF2196F3)
                            ServiceRequest.STATUS_COMPLETED   -> Color(0xFF4CAF50)
                            ServiceRequest.STATUS_CANCELLED   -> Color(0xFFD32F2F)
                            else                              -> Color.Transparent
                        }
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Header: member info + status badge ────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Gradient avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            BluePrimary.copy(alpha = 0.2f),
                                            BlueSecondary.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = request.memberName.firstOrNull()?.uppercase() ?: "M",
                                color = BluePrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = request.memberName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Service type chip
                            Surface(
                                color = BluePrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Build, null,
                                        modifier = Modifier.size(12.dp),
                                        tint = BluePrimary
                                    )
                                    Text(
                                        text = request.serviceType,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BluePrimary
                                    )
                                }
                            }
                        }
                    }

                    PremiumStatusBadge(request.status)
                }

                // ── Title ─────────────────────────────────────────────────
                if (request.title.isNotEmpty()) {
                    Text(
                        text = request.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        lineHeight = 22.sp
                    )
                }

                // ── Description ───────────────────────────────────────────
                if (request.description.isNotEmpty()) {
                    Text(
                        text = request.description,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 21.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // ── Date & time chips ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    request.preferredDate?.let {
                        PremiumInfoChip(
                            icon = Icons.Default.CalendarToday,
                            text = formatDate(it.toDate().time),
                            containerColor = Color(0xFFFFF3E0)
                        )
                    }
                    PremiumInfoChip(
                        icon = Icons.Default.AccessTime,
                        text = formatTimestamp(request.createdAt.toDate().time),
                        containerColor = Color(0xFFE3F2FD)
                    )
                }

                // ── Location row ──────────────────────────────────────────
                if (request.memberNeighborhood.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isSameNeighbourhood) BluePrimary else Color(0xFF999999)
                        )
                        Text(
                            text = request.memberNeighborhood,
                            fontSize = 12.sp,
                            color = if (isSameNeighbourhood) BluePrimary else Color(0xFF999999),
                            fontWeight = if (isSameNeighbourhood) FontWeight.SemiBold else FontWeight.Normal
                        )
                        // "Your neighbourhood" badge — only shown when it's a match
                        if (isSameNeighbourhood) {
                            Surface(
                                color = BluePrimary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Your neighbourhood",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // ── Divider before actions ────────────────────────────────
                if (request.status != ServiceRequest.STATUS_COMPLETED &&
                    request.status != ServiceRequest.STATUS_CANCELLED
                ) {
                    HorizontalDivider(thickness = 1.dp, color = BluePrimary.copy(alpha = 0.08f))
                }

                // ── Action buttons ────────────────────────────────────────
                when (request.status) {

                    ServiceRequest.STATUS_PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDecline,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFD32F2F)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                            ) {
                                Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Decline", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Gradient accept button
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(BluePrimary, BlueSecondary)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Accept", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    ServiceRequest.STATUS_ACCEPTED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDecline,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Decline", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onStartWork,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                elevation = ButtonDefaults.buttonElevation(4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Start Work", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ServiceRequest.STATUS_IN_PROGRESS -> {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            elevation = ButtonDefaults.buttonElevation(6.dp)
                        ) {
                            Icon(Icons.Default.Done, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mark as Completed", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    ServiceRequest.STATUS_COMPLETED -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Completed Successfully",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
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
                    }

                    ServiceRequest.STATUS_CANCELLED -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD32F2F).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Cancel, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Request Cancelled", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD32F2F))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composables (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumStatusBadge(status: String) {
    val (backgroundColor, textColor, icon, text) = when (status) {
        ServiceRequest.STATUS_PENDING     -> listOf(Color(0xFFFF9800).copy(alpha = 0.15f), Color(0xFFFF9800),    Icons.Default.Schedule,    "PENDING")
        ServiceRequest.STATUS_ACCEPTED    -> listOf(BluePrimary.copy(alpha = 0.15f),       BluePrimary,          Icons.Default.CheckCircle, "ACCEPTED")
        ServiceRequest.STATUS_IN_PROGRESS -> listOf(Color(0xFF2196F3).copy(alpha = 0.15f), Color(0xFF2196F3),    Icons.Default.Loop,        "IN PROGRESS")
        ServiceRequest.STATUS_COMPLETED   -> listOf(Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF4CAF50),    Icons.Default.Done,        "COMPLETED")
        ServiceRequest.STATUS_CANCELLED   -> listOf(Color(0xFFD32F2F).copy(alpha = 0.15f), Color(0xFFD32F2F),    Icons.Default.Cancel,      "CANCELLED")
        else                              -> listOf(Color(0xFF999999).copy(alpha = 0.15f), Color(0xFF999999),    Icons.Default.Info,        status.uppercase())
    }

    Surface(shape = RoundedCornerShape(10.dp), color = backgroundColor as Color) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, Modifier.size(14.dp), textColor as Color)
            Text(
                text = text as String,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun PremiumInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color
) {
    Surface(shape = RoundedCornerShape(10.dp), color = containerColor) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color(0xFF666666))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 3_600_000  -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else              -> "${diff / 86_400_000}d ago"
    }
}