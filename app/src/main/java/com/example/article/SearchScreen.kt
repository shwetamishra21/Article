package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

/* ---------- MODELS ---------- */

data class Member(
    val id: String,
    val name: String,
    val subtitle: String
)

data class Provider(
    val id: String,
    val name: String,
    val role: String
)

/* ---------- SCREEN ---------- */

@Composable
fun SearchScreen() {

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val members = listOf(
        Member("1", "Ravi Kumar", "Block A • Active"),
        Member("2", "Neha Sharma", "Block C • Online")
    )

    val providers = listOf(
        Provider("1", "Aman", "Electrician"),
        Provider("2", "Suresh", "Plumber"),
        Provider("3", "Kiran", "Cleaner")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        /* ---------- HEADER ---------- */
        Text(
            text = "Search",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 22.sp
        )

        Spacer(Modifier.height(12.dp))

        /* ---------- SEARCH FIELD ---------- */
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search members or services") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        /* ---------- TABS ---------- */
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Members") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Service Providers") }
            )
        }

        Spacer(Modifier.height(16.dp))

        /* ---------- CONTENT ---------- */
        when (selectedTab) {
            0 -> MembersList(
                members = members.filter {
                    it.name.contains(query, true)
                }
            )

            1 -> ProvidersList(
                providers = providers.filter {
                    it.name.contains(query, true) ||
                            it.role.contains(query, true)
                }
            )
        }
    }
}

/* ---------- MEMBERS ---------- */

@Composable
private fun MembersList(
    members: List<Member>
) {
    if (members.isEmpty()) {
        EmptySearchState("No members found")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(members, key = { it.id }) { member ->
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // future → open profile / DM
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(member.name, fontWeight = FontWeight.Bold)
                        Text(
                            member.subtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/* ---------- PROVIDERS ---------- */

@Composable
private fun ProvidersList(
    providers: List<Provider>
) {
    if (providers.isEmpty()) {
        EmptySearchState("No providers found")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(providers, key = { it.id }) { provider ->
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // future → request / chat provider
                    }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(provider.name, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        provider.role,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/* ---------- EMPTY ---------- */

@Composable
private fun EmptySearchState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(text, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}
