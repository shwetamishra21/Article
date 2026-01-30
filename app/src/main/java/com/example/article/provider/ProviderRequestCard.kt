package com.example.article.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.article.Repository.ServiceRequest

@Composable
fun ProviderRequestCard(
    request: ServiceRequest,
    onAccept: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            /* ---------- TITLE ---------- */
            Text(
                text = request.title,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(4.dp))

            /* ---------- META ---------- */
            Text(
                text = request.serviceType,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            /* ---------- ACTION ---------- */
            when (request.status) {

                ServiceRequest.STATUS_PENDING -> {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Accept Request")
                    }
                }

                ServiceRequest.STATUS_ACCEPTED -> {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark Completed")
                    }
                }

                ServiceRequest.STATUS_COMPLETED -> {
                    Text(
                        text = "Completed",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                ServiceRequest.STATUS_CANCELLED -> {
                    Text(
                        text = "Cancelled",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
