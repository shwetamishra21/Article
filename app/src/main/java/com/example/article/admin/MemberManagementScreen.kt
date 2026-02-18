package com.example.article.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemberManagementViewModel = viewModel()
) {
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMembers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Member Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text(
                    text = error ?: "Unknown error",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
                members.isEmpty() -> Text(
                    text = "No members found.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(members) { member ->
                        MemberListItem(
                            member = member,
                            onToggleBan = { viewModel.toggleBan(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberListItem(
    member: MemberItem,
    onToggleBan: (MemberItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = member.name, style = MaterialTheme.typography.titleMedium)
                Text(text = member.email, style = MaterialTheme.typography.bodySmall)
                Text(text = member.neighbourhood, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onToggleBan(member) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (member.isBanned)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (member.isBanned) "Unban" else "Ban")
            }
        }
    }
}