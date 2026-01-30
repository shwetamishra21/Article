package com.example.article

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import java.util.UUID

/* ---------------- MODEL ---------------- */

data class MyPost(
    val id: String,
    val content: String,
    val time: String,
    val likes: Int
)

/* ---------------- PROFILE SCREEN ---------------- */

@Composable
fun ProfileScreen(
    role: UserRole,          // ✅ ENUM (DAY 6 SAFE)
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf("User") }
    var bio by remember { mutableStateOf("Living in the neighbourhood 💙") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    var myPosts by remember {
        mutableStateOf(
            listOf(
                MyPost(UUID.randomUUID().toString(), "Happy to be part of this community!", "2h ago", 6),
                MyPost(UUID.randomUUID().toString(), "Anyone up for morning walks?", "1d ago", 12)
            )
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> profileImageUri = uri }

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        /* ---------- PROFILE HEADER ---------- */
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            profileImageUri
                                ?: "https://cdn-icons-png.flaticon.com/512/149/149071.png"
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (isEditing) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                } else {
                    Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(6.dp))

                if (isEditing) {
                    BasicTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    )
                } else {
                    Text(
                        bio,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                /* ---------- ROLE BADGE ---------- */
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            when (role) {
                                UserRole.ADMIN -> "Admin Account"
                                UserRole.SERVICE_PROVIDER -> "Service Provider"
                                UserRole.MEMBER -> "Member Account"
                            },
                            fontSize = 12.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (role) {
                            UserRole.ADMIN ->
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            UserRole.SERVICE_PROVIDER ->
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            UserRole.MEMBER ->
                                MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isEditing) {
                        Button(onClick = { isEditing = false }) { Text("Save") }
                        OutlinedButton(onClick = { isEditing = false }) { Text("Cancel") }
                    } else {
                        Button(onClick = { isEditing = true }) { Text("Edit Profile") }
                        OutlinedButton(onClick = onLogout) { Text("Logout") }
                    }
                }
            }
        }

        /* ---------- MY POSTS HEADER ---------- */
        item {
            Text(
                text = "My Posts",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        /* ---------- MY POSTS ---------- */
        items(
            items = myPosts,
            key = { it.id }
        ) { post ->
            MyPostCard(
                post = post,
                onLike = {
                    myPosts = myPosts.map {
                        if (it.id == post.id)
                            it.copy(likes = it.likes + 1)
                        else it
                    }
                }
            )
        }
    }
}

/* ---------------- POST CARD ---------------- */

@Composable
private fun MyPostCard(
    post: MyPost,
    onLike: () -> Unit
) {
    var liked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                post.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(onClick = {
                    liked = !liked
                    if (liked) onLike()
                }) {
                    Icon(
                        imageVector = if (liked)
                            Icons.Filled.Favorite
                        else
                            Icons.Filled.FavoriteBorder,
                        contentDescription = null
                    )
                }

                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    "${post.likes}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                Text(
                    post.time,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
