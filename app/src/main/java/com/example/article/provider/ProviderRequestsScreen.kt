package com.example.article.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.ProviderRequestsViewModel
import com.example.article.Repository.ServiceRequest
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProviderRequestsScreen(
    viewModel: ProviderRequestsViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val providerId = auth.currentUser?.uid

    val requests by viewModel.requests.collectAsState()
    val loading by viewModel.loading.collectAsState()

    // 🔒 Guard: not logged in
    if (providerId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Not authenticated", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LaunchedEffect(Unit) {
        viewModel.loadRequests(providerId)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        /* ---------- HEADER ---------- */
        Text(
            text = "Assigned Requests",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        /* ---------- LOADING ---------- */
        if (loading && requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        /* ---------- EMPTY ---------- */
        if (!loading && requests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No requests assigned yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        /* ---------- LIST ---------- */
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = requests,
                key = { it.id }
            ) { request ->
                ProviderRequestCard(
                    request = request,
                    onAccept = {
                        viewModel.accept(request.id, providerId)
                    },
                    onComplete = {
                        viewModel.complete(request.id, providerId)
                    }
                )
            }
        }
    }
}
