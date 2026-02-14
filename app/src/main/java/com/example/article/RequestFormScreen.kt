package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose. foundation. clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.article.Repository.MemberRequestViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFormScreen(
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    viewModel: MemberRequestViewModel = viewModel()
) {
    var serviceType by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser by UserSessionManager.currentUser.collectAsState()

    // Expanded list of service types
    val serviceTypes = listOf(
        "Plumber",
        "Electrician",
        "Cleaner",
        "Carpenter",
        "Painter",
        "Gardener",
        "AC Repair",
        "Appliance Repair",
        "Pest Control",
        "Locksmith",
        "Handyman",
        "Mason",
        "Welder",
        "Tailor",
        "Beautician",
        "Tutor",
        "Chef/Cook",
        "Driver",
        "Security Guard",
        "Moving & Packing",
        "Interior Designer",
        "Solar Panel Installer",
        "Water Tank Cleaner",
        "Car Wash",
        "Other"
    )

    val dateState = rememberDatePickerState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New Service Request",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Type Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = serviceType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Type *", fontSize = 13.sp) },
                    placeholder = { Text("Select a service", fontSize = 13.sp) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    serviceTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, fontSize = 13.sp) },
                            onClick = {
                                serviceType = type
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Request Title *", fontSize = 13.sp) },
                placeholder = { Text("e.g., Fix leaking kitchen sink", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *", fontSize = 13.sp) },
                placeholder = { Text("Describe the service you need in detail...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )

            // Date Picker Field
            OutlinedTextField(
                value = selectedDate?.let {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferred Date", fontSize = 13.sp) },
                placeholder = { Text("Select a date (optional)", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (selectedDate != null) {
                        TextButton(onClick = { selectedDate = null }) {
                            Text("Clear", fontSize = 11.sp)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Error Message
            error?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFB71C1C).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFB71C1C),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Submit Button
            val isEnabled = !loading && serviceType.isNotBlank() &&
                    title.isNotBlank() && description.isNotBlank()

            Button(
                onClick = {
                    if (currentUser == null) {
                        viewModel.setError("You must be logged in")
                        return@Button
                    }

                    if (serviceType.isBlank() || title.isBlank() || description.isBlank()) {
                        viewModel.setError("Please fill all required fields")
                        return@Button
                    }

                    viewModel.createRequest(
                        title = title,
                        description = description,
                        serviceType = serviceType,
                        preferredDate = selectedDate,
                        onSuccess = {
                            onSubmit()
                        }
                    )
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = if (isEnabled) {
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                    } else Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            "Submit Request",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEnabled) Color.White else Color(0xFF666666)
                        )
                    }
                }
            }

            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !loading
            ) {
                Text(
                    "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            // Only allow future dates (including today)
                            if (millis >= System.currentTimeMillis() - 86400000) {
                                selectedDate = Date(millis)
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}