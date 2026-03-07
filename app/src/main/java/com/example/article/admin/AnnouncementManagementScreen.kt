package com.example.article.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.AdminAnnouncement
import com.example.article.Repository.AnnouncementViewModel
import com.example.article.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnnouncementViewModel = viewModel()
) {
    val announcements by viewModel.announcements.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<AdminAnnouncement?>(null) }

    LaunchedEffect(Unit) { viewModel.loadAnnouncements() }
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    showDeleteDialog?.let { announcement ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFCC4444)) },
            title = { Text("Delete Announcement?") },
            text = { Text("\"${announcement.title}\" will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAnnouncement(announcement); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4444))
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }

    if (showCreateDialog) {
        CreateAnnouncementDialog(
            isLoading = loading,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, content, isPinned ->
                viewModel.createAnnouncement(title, content, isPinned)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Announcements", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text(
                            "${announcements.size} total · ${announcements.count { it.isPinned }} pinned",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "New", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .background(Brush.linearGradient(colors = listOf(BluePrimary, BlueSecondary)))
                    .shadow(elevation = 6.dp, spotColor = BluePrimary.copy(alpha = 0.4f))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Campaign, null) },
                text = { Text("New", fontWeight = FontWeight.SemiBold) },
                containerColor = BluePrimary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            when {
                loading && announcements.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BluePrimary)
                }
                announcements.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Campaign, null, modifier = Modifier.size(56.dp), tint = BluePrimary.copy(alpha = 0.4f))
                        Text("No announcements yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                        Text("Create your first announcement\nfor all neighbourhood members.", fontSize = 13.sp, color = Color(0xFF888888))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = BluePrimary.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Info, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                    Text(
                                        "Pinned announcements appear at the top of the Home screen.",
                                        fontSize = 12.sp, color = BluePrimary, lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        items(announcements, key = { it.id }) { announcement ->
                            AnnouncementCard(
                                announcement = announcement,
                                dateLabel = if (announcement.createdDate > 0L)
                                    dateFmt.format(Date(announcement.createdDate)) else "—",
                                onPin = { viewModel.togglePin(announcement) },
                                onDelete = { showDeleteDialog = announcement }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: AdminAnnouncement,
    dateLabel: String,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (announcement.isPinned) 4.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (announcement.isPinned) BluePrimary.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (announcement.isPinned) BluePrimary.copy(alpha = 0.06f) else SurfaceLight
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (announcement.isPinned) BluePrimary.copy(alpha = 0.15f) else Color(0xFF888888).copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Campaign, null,
                        tint = if (announcement.isPinned) BluePrimary else Color(0xFF888888),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(announcement.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                    Text(dateLabel, fontSize = 11.sp, color = Color(0xFF999999))
                }
                if (announcement.isPinned) {
                    Icon(Icons.Default.PushPin, "Pinned", tint = BluePrimary, modifier = Modifier.size(18.dp))
                }
            }

            Text(announcement.content, fontSize = 13.sp, color = Color(0xFF555555), lineHeight = 19.sp, maxLines = 3)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (announcement.isPinned) Color(0xFF888888) else BluePrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (announcement.isPinned) Color(0xFFDDDDDD) else BluePrimary.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        if (announcement.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        null, modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(if (announcement.isPinned) "Unpin" else "Pin", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCC4444).copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Delete", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CreateAnnouncementDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(true) }
    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = SurfaceLight, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(BluePrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Campaign, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("New Announcement", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        Text("Visible to all neighbourhood members", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Title *") },
                    isError = titleError,
                    supportingText = if (titleError) { { Text("Title is required", color = MaterialTheme.colorScheme.error) } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    singleLine = true,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    minLines = 3, maxLines = 5,
                    enabled = !isLoading
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = BluePrimary),
                        enabled = !isLoading
                    )
                    Text("Pin to top of Home screen", fontSize = 13.sp, color = OnSurfaceLight)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp), enabled = !isLoading
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isBlank()) titleError = true
                            else onConfirm(title.trim(), content.trim(), isPinned)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Post", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}