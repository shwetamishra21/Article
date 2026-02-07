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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFormScreen(
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    var serviceType by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val serviceTypes = listOf(
        "Plumber",
        "Electrician",
        "Cleaner",
        "Carpenter",
        "Painter",
        "Gardener",
        "AC Repair",
        "Other"
    )

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

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
                    label = { Text("Service Type", fontSize = 13.sp) },
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
                    onDismissRequest = { expanded = false }
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
                label = { Text("Request Title", fontSize = 13.sp) },
                placeholder = { Text("e.g., Fix leaking pipe", fontSize = 13.sp) },
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
                label = { Text("Description", fontSize = 13.sp) },
                placeholder = { Text("Describe the service you need...", fontSize = 13.sp) },
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

            // Preferred Date
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Preferred Date", fontSize = 13.sp) },
                placeholder = { Text("e.g., Tomorrow, Next Monday", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
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
                    val user = auth.currentUser
                    if (user == null) {
                        error = "You must be logged in"
                        return@Button
                    }

                    if (serviceType.isBlank() || title.isBlank() || description.isBlank()) {
                        error = "Please fill all required fields"
                        return@Button
                    }

                    loading = true
                    error = null

                    val requestData = hashMapOf(
                        "serviceType" to serviceType,
                        "title" to title,
                        "description" to description,
                        "date" to date.ifBlank { "ASAP" },
                        "status" to "Pending",
                        "createdBy" to user.uid,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("service_requests")
                        .add(requestData)
                        .addOnSuccessListener {
                            loading = false
                            onSubmit()
                        }
                        .addOnFailureListener { exception ->
                            loading = false
                            error = exception.localizedMessage ?: "Failed to submit request"
                        }
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
}