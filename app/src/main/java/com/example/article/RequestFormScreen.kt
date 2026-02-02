package com.example.article

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFormScreen(
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val services = listOf(
        "Plumber",
        "Electrician",
        "Cleaner",
        "Carpenter",
        "Painter",
        "Other"
    )

    var selectedService by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("Select date") }
    var showServiceMenu by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            selectedDate = "$day/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Service Request",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /* ---------- SERVICE DROPDOWN ---------- */
            Text(
                text = "Select Service",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            ExposedDropdownMenuBox(
                expanded = showServiceMenu,
                onExpandedChange = { showServiceMenu = it }
            ) {
                OutlinedTextField(
                    value = selectedService,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(), // ← CRITICAL for dropdown
                    readOnly = true,
                    placeholder = { Text("Choose a service") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = showServiceMenu
                        )
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = showServiceMenu,
                    onDismissRequest = { showServiceMenu = false }
                ) {
                    services.forEach { service ->
                        DropdownMenuItem(
                            text = { Text(service) },
                            onClick = {
                                selectedService = service
                                showServiceMenu = false
                            }
                        )
                    }
                }
            }

            /* ---------- TITLE ---------- */
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Request Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            /* ---------- DESCRIPTION ---------- */
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                maxLines = 6,
                shape = RoundedCornerShape(12.dp)
            )

            /* ---------- DATE ---------- */
            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(selectedDate)
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.weight(1f))

            /* ---------- ACTIONS ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    enabled = !loading && selectedService.isNotBlank() &&
                            title.isNotBlank() && description.isNotBlank() &&
                            selectedDate != "Select date",
                    onClick = {
                        val user = auth.currentUser
                        if (user == null) {
                            error = "Not logged in"
                            return@Button
                        }

                        loading = true
                        error = null

                        firestore.collection("requests")
                            .add(
                                mapOf(
                                    "serviceType" to selectedService,
                                    "title" to title,
                                    "description" to description,
                                    "date" to selectedDate,
                                    "status" to "pending",
                                    "createdBy" to user.uid,
                                    "createdAt" to System.currentTimeMillis()
                                )
                            )
                            .addOnSuccessListener {
                                loading = false
                                onSubmit()
                            }
                            .addOnFailureListener {
                                loading = false
                                error = it.localizedMessage
                            }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (loading) "Submitting…" else "Submit")
                }
            }
        }
    }
}
