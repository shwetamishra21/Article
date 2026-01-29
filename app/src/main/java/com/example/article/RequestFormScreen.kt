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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun RequestFormScreen(
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
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

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(16.dp)
    ) {

        /* ---------- HEADER ---------- */
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "New Service Request",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(24.dp))

        /* ---------- SERVICE DROPDOWN ---------- */
        Text("Who do you want to hire?", fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))

        Box {
            OutlinedTextField(
                value = selectedService,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showServiceMenu = true },
                readOnly = true,
                placeholder = { Text("Select service") },
                trailingIcon = {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            )

            DropdownMenu(
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

        Spacer(Modifier.height(20.dp))

        /* ---------- TITLE ---------- */
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Problem title") },
            placeholder = { Text("Short summary of the issue") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        /* ---------- DESCRIPTION ---------- */
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Problem description") },
            placeholder = { Text("Describe the problem in detail") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 6
        )

        Spacer(Modifier.height(20.dp))

        /* ---------- DATE PICKER ---------- */
        OutlinedButton(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(selectedDate)
        }

        Spacer(Modifier.height(32.dp))

        /* ---------- ACTIONS ---------- */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (
                        selectedService.isNotBlank() &&
                        title.isNotBlank() &&
                        description.isNotBlank() &&
                        selectedDate != "Select date"
                    ) {
                        onSubmit()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Submit Request")
            }
        }
    }
}
