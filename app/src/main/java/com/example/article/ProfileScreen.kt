package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.article.ui.theme.*

@Composable
fun ProfileScreen(
    username: String,
    onUsernameChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var editableUsername by remember { mutableStateOf(username) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(LightIvory, SoftLilac, RoyalViolet)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(60),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            modifier = Modifier.size(110.dp),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = AppWhite,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Your Profile",
            fontSize = 24.sp,
            color = AppWhite,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editableUsername,
            onValueChange = { editableUsername = it },
            label = { Text("Username", color = AppWhite) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppWhite,
                unfocusedBorderColor = SoftLilac,
                cursorColor = AppWhite
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onUsernameChange(editableUsername) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save", color = AppWhite)
        }

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = AppWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", color = AppWhite)
        }
    }
}
