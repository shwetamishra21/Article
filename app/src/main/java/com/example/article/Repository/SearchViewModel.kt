package com.example.article.Repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SearchableUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "member",
    val neighbourhood: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val serviceType: String = "", // For providers only
    val rating: Float = 5.0f, // For providers only (default 5.0)
    val completedJobs: Int = 0, // For providers only
    val isAvailable: Boolean = false, // For providers only
    val createdAt: Timestamp = Timestamp.now()
)

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val users: List<SearchableUser>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private var allUsers: List<SearchableUser> = emptyList()
    private var currentSearchQuery: String = ""

    companion object {
        private const val TAG = "SearchViewModel"
    }

    /**
     * Load all users (members, providers, admins) with enhanced provider data
     */
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _uiState.value = SearchUiState.Loading

                val snapshot = firestore.collection("users")
                    .get()
                    .await()

                val users = snapshot.documents.mapNotNull { doc ->
                    try {
                        mapDocToSearchableUser(doc)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping user ${doc.id}", e)
                        null
                    }
                }

                // Load provider stats asynchronously for service providers
                val enhancedUsers = users.map { user ->
                    if (user.role == "service_provider") {
                        enhanceProviderData(user)
                    } else {
                        user
                    }
                }

                allUsers = enhancedUsers

                // Apply current filters after loading
                applyFilters()

                Log.d(TAG, "Loaded ${enhancedUsers.size} users")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users", e)
                _uiState.value = SearchUiState.Error(e.message ?: "Failed to load users")
            }
        }
    }

    /**
     * Enhance provider data with real stats from service_requests
     */
    private suspend fun enhanceProviderData(user: SearchableUser): SearchableUser {
        return try {
            // Get completed jobs count
            val completedSnapshot = firestore.collection("service_requests")
                .whereEqualTo("providerId", user.uid)
                .whereEqualTo("status", "completed")
                .get()
                .await()

            val completedJobs = completedSnapshot.size()

            // Get user document for latest data
            val userDoc = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()

            user.copy(
                completedJobs = completedJobs,
                isAvailable = userDoc.getBoolean("isAvailable") ?: false,
                rating = (userDoc.getDouble("rating") ?: 5.0).toFloat(),
                serviceType = userDoc.getString("serviceType") ?: user.serviceType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error enhancing provider ${user.uid}", e)
            user // Return original on error
        }
    }

    /**
     * Search users by query with smart filtering
     */
    fun searchUsers(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    /**
     * Filter by category (All, Members, Providers, or specific service type)
     */
    fun setCategory(category: String) {
        _selectedCategory.value = category
        applyFilters()
    }

    /**
     * CRITICAL FIX: Apply both category and search filters together
     * This ensures the filters work correctly and don't interfere with each other
     */
    private fun applyFilters() {
        var filtered = allUsers

        // STEP 1: Apply category filter (role-based)
        filtered = when (_selectedCategory.value) {
            "All" -> filtered // Show everyone
            "Members" -> filtered.filter { it.role == "member" } // Only members
            "Providers" -> filtered.filter { it.role == "service_provider" } // Only providers
            "Admins" -> filtered.filter { it.role == "admin" } // Only admins
            else -> {
                // Filter by specific service type (e.g., "Plumber", "Electrician")
                filtered.filter {
                    it.role == "service_provider" &&
                            it.serviceType == _selectedCategory.value
                }
            }
        }

        // STEP 2: Apply search query filter (text-based)
        if (currentSearchQuery.isNotBlank()) {
            filtered = filtered.filter { user ->
                user.name.contains(currentSearchQuery, ignoreCase = true) ||
                        user.neighbourhood.contains(currentSearchQuery, ignoreCase = true) ||
                        user.serviceType.contains(currentSearchQuery, ignoreCase = true) ||
                        user.bio.contains(currentSearchQuery, ignoreCase = true) ||
                        user.email.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Update UI state with filtered results
        _uiState.value = SearchUiState.Success(filtered)

        Log.d(TAG, "Applied filters - Category: ${_selectedCategory.value}, Query: '$currentSearchQuery', Results: ${filtered.size}")
    }

    /**
     * Get categories for filter chips (includes top service types)
     */
    fun getCategories(): List<String> {
        val baseCategories = listOf("All", "Members", "Providers")

        // Optionally add top service types
        val serviceTypes = allUsers
            .filter { it.role == "service_provider" && it.serviceType.isNotEmpty() }
            .groupBy { it.serviceType }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        return baseCategories // You can add "+ serviceTypes" to show service type filters
    }

    /**
     * Map Firestore document to SearchableUser
     */
    private fun mapDocToSearchableUser(doc: DocumentSnapshot): SearchableUser {
        return SearchableUser(
            uid = doc.id,
            name = doc.getString("name") ?: "",
            email = doc.getString("email") ?: "",
            role = doc.getString("role") ?: "member",
            neighbourhood = doc.getString("neighbourhood") ?: "",
            bio = doc.getString("bio") ?: "",
            photoUrl = doc.getString("photoUrl") ?: "",
            serviceType = doc.getString("serviceType") ?: "",
            rating = (doc.getDouble("rating") ?: 5.0).toFloat(),
            completedJobs = (doc.getLong("completedJobs") ?: 0L).toInt(),
            isAvailable = doc.getBoolean("isAvailable") ?: false,
            createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
        )
    }

    /**
     * Refresh user list (useful for pull-to-refresh)
     */
    fun refresh() {
        loadUsers()
    }
}