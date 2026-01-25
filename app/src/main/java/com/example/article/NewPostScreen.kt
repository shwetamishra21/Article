package com.example.article

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage

enum class PostType {
    POST, ANNOUNCEMENT
}

@Composable
fun NewPostScreen(
    onPostUploaded: () -> Unit
) {
    var selectedType by remember { mutableStateOf(PostType.POST) }
    var caption by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFFE3F2FD)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {

        /* ---------- HEADER ---------- */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Create",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1)
            )
            Text(
                text = "Share with your neighbourhood",
                fontSize = 14.sp,
                color = Color(0xFF546E7A)
            )
        }

        Spacer(Modifier.height(12.dp))

        /* ---------- TYPE TOGGLE ---------- */
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(6.dp)
        ) {
            PostTypeTab(
                text = "Post",
                selected = selectedType == PostType.POST,
                onClick = { selectedType = PostType.POST },
                modifier = Modifier.weight(1f)
            )
            PostTypeTab(
                text = "Announcement",
                selected = selectedType == PostType.ANNOUNCEMENT,
                onClick = { selectedType = PostType.ANNOUNCEMENT },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* ---------- IMAGE (POST ONLY) ---------- */
            if (selectedType == PostType.POST) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { imagePicker.launch("image/*") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFF42A5F5),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Add photo",
                                    color = Color(0xFF546E7A),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            /* ---------- TEXT ---------- */
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = {
                    Text(
                        if (selectedType == PostType.POST)
                            "Write something for your neighbours…"
                        else
                            "Write an announcement…"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(12.dp))

            /* ---------- SUBMIT ---------- */
            Button(
                onClick = onPostUploaded,
                enabled = caption.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF42A5F5)
                )
            ) {
                Icon(
                    if (selectedType == PostType.POST)
                        Icons.Default.Image
                    else
                        Icons.Default.Campaign,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (selectedType == PostType.POST)
                        "Post"
                    else
                        "Publish Announcement",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/* ---------- TAB ---------- */

@Composable
private fun PostTypeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = if (selected) 6.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0xFF42A5F5),
                spotColor = Color(0xFF42A5F5)
            )
            .background(
                if (selected) Color(0xFF42A5F5) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF1E88E5),
            fontWeight = FontWeight.Medium
        )
    }
}
