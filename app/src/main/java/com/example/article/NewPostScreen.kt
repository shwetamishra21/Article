package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NewPostScreen(
    onPostUploaded: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var showSuccess by remember { mutableStateOf(false) }

    val categories = listOf(
        "General",
        "Electrician",
        "Plumber",
        "Cleaning",
        "Carpenter",
        "Other"
    )

    if (showSuccess) {
        SuccessState(onDone = onPostUploaded)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        /* ---------- HEADER ---------- */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onPostUploaded) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "New Post",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        /* ---------- TITLE ---------- */
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            placeholder = { Text("Short summary") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        /* ---------- DESCRIPTION ---------- */
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            placeholder = { Text("Explain the issue or post details") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(16.dp))

        /* ---------- CATEGORY ---------- */
        Text("Category", fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(8.dp))

        CategoryRow(
            categories = categories,
            selected = category,
            onSelect = { category = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        /* ---------- SUBMIT ---------- */
        Button(
            onClick = {
                if (title.isNotBlank() && description.isNotBlank()) {
                    showSuccess = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = title.isNotBlank() && description.isNotBlank(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Post",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ---------- CATEGORY ROW ---------- */

@Composable
private fun CategoryRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        categories.forEach { item ->
            FilterChip(
                selected = selected == item,
                onClick = { onSelect(item) },
                label = { Text(item) }
            )
        }
    }
}

/* ---------- SUCCESS STATE ---------- */

@Composable
private fun SuccessState(
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = "Post Created",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your post has been added successfully",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onDone) {
                Text("Back to Home")
            }
        }
    }
}
