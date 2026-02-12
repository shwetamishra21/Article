package com.example.article.Repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.article.utils.CloudinaryHelper
import com.example.article.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreatePostViewModel(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private companion object {
        const val TAG = "CreatePostViewModel"
    }

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()

    private val _uploadedImagePublicId = MutableStateFlow<String?>(null)

    fun createPost(
        type: String,
        content: String,
        title: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isCreating.value = true

                if (content.isBlank()) {
                    _errorMessage.value = "Content cannot be empty"
                    _isCreating.value = false
                    return@launch
                }

                val currentProfile = UserSessionManager.currentUser.value
                if (currentProfile == null) {
                    _errorMessage.value = "User profile not loaded"
                    _isCreating.value = false
                    return@launch
                }

                val postRef = firestore.collection("posts").document()
                val postId = postRef.id

                val post = hashMapOf(
                    "id" to postId,
                    "type" to type,
                    "authorId" to currentProfile.uid,
                    "authorName" to currentProfile.name,
                    "authorPhotoUrl" to currentProfile.photoUrl,
                    "content" to content.trim(),
                    "title" to title?.trim(),
                    "imageUrl" to _uploadedImageUrl.value,
                    "imagePublicId" to _uploadedImagePublicId.value,
                    "likes" to 0,
                    "likedBy" to emptyMap<String, Boolean>(),
                    "commentCount" to 0,
                    "createdAt" to Timestamp.now()
                )

                postRef.set(post).await()

                _isCreating.value = false
                _successMessage.value =
                    if (type == "announcement") "Announcement posted!"
                    else "Post created!"

                clearImageUpload()

            } catch (e: Exception) {
                _isCreating.value = false
                _errorMessage.value = "Failed to create post: ${e.message}"
            }
        }
    }

    fun uploadPostImage(imageUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _isUploadingImage.value = true

                if (!CloudinaryHelper.isConfigured()) {
                    _isUploadingImage.value = false
                    _errorMessage.value = "Cloudinary not configured"
                    return@launch
                }

                val uploadResult = CloudinaryHelper.uploadImage(
                    imageUri = imageUri,
                    context = context,
                    folder = "posts"
                )

                if (uploadResult.isFailure) {
                    _isUploadingImage.value = false
                    _errorMessage.value =
                        "Image upload failed: ${uploadResult.exceptionOrNull()?.message}"
                    return@launch
                }

                val cloudinaryResult = uploadResult.getOrNull()!!

                _uploadedImageUrl.value = cloudinaryResult.secureUrl
                _uploadedImagePublicId.value = cloudinaryResult.publicId
                _isUploadingImage.value = false

            } catch (e: Exception) {
                _isUploadingImage.value = false
                _errorMessage.value = "Error uploading image: ${e.message}"
            }
        }
    }

    fun clearImageUpload() {
        _uploadedImageUrl.value = null
        _uploadedImagePublicId.value = null
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
