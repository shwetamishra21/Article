package com.example.article.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.AdminMember
import com.example.article.Repository.MemberManagementViewModel
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemberManagementViewModel = viewModel()
) {
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showRemoveDialog by remember { mutableStateOf<AdminMember?>(null) }
    var showBanDialog by remember { mutableStateOf<AdminMember?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadMembers() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val filteredMembers = if (searchQuery.isBlank()) members
    else members.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
    }

    showRemoveDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            icon = { Icon(Icons.Default.PersonRemove, null, tint = Color(0xFFCC4444)) },
            title = { Text("Remove Member?") },
            text = { Text("${member.name} will be removed from the neighbourhood. They can re-apply to join.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.removeMember(member); showRemoveDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4444))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel") }
            }
        )
    }

    showBanDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showBanDialog = null },
            icon = {
                Icon(
                    if (member.isBanned) Icons.Default.LockOpen else Icons.Default.Block,
                    null,
                    tint = if (member.isBanned) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            },
            title = { Text(if (member.isBanned) "Unban Member?" else "Ban Member?") },
            text = {
                Text(
                    if (member.isBanned)
                        "${member.name} will be allowed to participate in the community again."
                    else
                        "${member.name} will be banned and unable to post or interact in the community."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleBan(member); showBanDialog = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (member.isBanned) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                ) { Text(if (member.isBanned) "Unban" else "Ban") }
            },
            dismissButton = {
                TextButton(onClick = { showBanDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Member Management", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        if (members.isNotEmpty()) {
                            Text("${members.size} member${if (members.size != 1) "s" else ""}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(Brush.linearGradient(colors = listOf(BluePrimary, BlueSecondary)))
                    .shadow(elevation = 6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            when {
                isLoading && members.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BluePrimary
                    )
                }

                error != null && members.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = Color(0xFFCC4444))
                        Text(error ?: "Error", color = Color(0xFFCC4444), fontSize = 14.sp)
                        Button(
                            onClick = { viewModel.loadMembers() },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) { Text("Retry") }
                    }
                }

                members.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Group, null, modifier = Modifier.size(56.dp), tint = BluePrimary.copy(alpha = 0.4f))
                        Text("No members yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                        Text("Members who join your neighbourhood\nwill appear here.", fontSize = 13.sp, color = Color(0xFF888888))
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search members…", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF999999)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, null, tint = Color(0xFF999999))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    unfocusedContainerColor = SurfaceLight,
                                    focusedContainerColor = SurfaceLight
                                ),
                                singleLine = true
                            )
                        }

                        item {
                            Text(
                                "${filteredMembers.size} member${if (filteredMembers.size != 1) "s" else ""}${if (searchQuery.isNotEmpty()) " found" else ""}",
                                fontSize = 12.sp,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        items(filteredMembers, key = { it.id }) { member ->
                            MemberCard(
                                member = member,
                                onBan = { showBanDialog = member },
                                onRemove = { showRemoveDialog = member }
                            )
                        }

                        if (filteredMembers.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No members match \"$searchQuery\"", color = Color(0xFF888888), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: AdminMember,
    onBan: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = BluePrimary.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        color = if (member.isBanned) Color(0xFFFFF3E0) else SurfaceLight
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BluePrimary.copy(alpha = 0.2f),
                                    BlueSecondary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(member.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                    Text(member.email, fontSize = 12.sp, color = Color(0xFF666666))
                }

                if (member.isBanned) {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Banned",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCC4444).copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onBan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (member.isBanned) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                ) {
                    Icon(
                        if (member.isBanned) Icons.Default.LockOpen else Icons.Default.Block,
                        null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (member.isBanned) "Unban" else "Ban",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}