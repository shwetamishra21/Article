package com.example.article

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(text = "Search Articles", fontSize = 20.sp, color = DeepPlum)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RoyalViolet,
                unfocusedBorderColor = SoftLilac,
                cursorColor = RoyalViolet
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Results will appear here.", fontSize = 14.sp, color = SteelGray)
    }
}
