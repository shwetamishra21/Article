package com.example.article

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryHelper {

    private var isInitialized = false
    private const val CLOUD_NAME = "dpn1202gn"  // Hardcoded (safe for URL gen, public info)

    fun initIfNeeded(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to CLOUD_NAME,
                "secure" to true
            )
            MediaManager.init(context, config)
            isInitialized = true
        }
    }

    suspend fun uploadPostImage(
        uri: Uri,
        userId: String,
        onProgress: (Float) -> Unit = {}
    ): String = suspendCancellableCoroutine { continuation ->
        val requestId = MediaManager.get().upload(uri)
            .option("folder", "posts/$userId")
            .option("resource_type", "image")
            .unsigned("dpn1202gn_preset")  // Move to end, no transformations after
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    onProgress(0.1f)
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = bytes.toFloat() / totalBytes.toFloat()
                    onProgress(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    onProgress(1.0f)
                    val secureUrl = resultData["secure_url"] as? String
                    if (secureUrl != null) {
                        continuation.resume(secureUrl)
                    } else {
                        continuation.resumeWithException(
                            Exception("Failed to get image URL from upload result")
                        )
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(
                        Exception("Upload failed: ${error.description}")
                    )
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) { }
            })
            .dispatch()

        continuation.invokeOnCancellation {
            MediaManager.get().cancelRequest(requestId)
        }
    }


    suspend fun uploadProfileImage(
        uri: Uri,
        userId: String,
        onProgress: (Float) -> Unit = {}
    ): String = suspendCancellableCoroutine { continuation ->
        val requestId = MediaManager.get().upload(uri)
            .option("folder", "profiles/$userId")
            .option("resource_type", "image")
            .option("public_id", "avatar_$userId")
            .option("overwrite", true)
            .option("quality", "auto:best")
            .option("fetch_format", "auto")
            .option("quality", "auto:good")  // lighter than "auto:best"
            .option("width", 1024)           // Resize before upload
            .option("crop", "limit")
            .unsigned("dpn1202gn_preset")  // Replace with your actual preset
            .option(
                "transformation",
                listOf(
                    mapOf(
                        "width" to 500,
                        "height" to 500,
                        "crop" to "fill",
                        "gravity" to "face",
                        "quality" to "auto:best",
                        "fetch_format" to "auto"
                    )
                )
            )
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    onProgress(0.1f)
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = bytes.toFloat() / totalBytes.toFloat()
                    onProgress(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    onProgress(1.0f)
                    val secureUrl = resultData["secure_url"] as? String
                    if (secureUrl != null) {
                        continuation.resume(secureUrl)
                    } else {
                        continuation.resumeWithException(
                            Exception("Failed to get image URL from upload result")
                        )
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(
                        Exception("Upload failed: ${error.description}")
                    )
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) { }
            })
            .dispatch()

        continuation.invokeOnCancellation {
            MediaManager.get().cancelRequest(requestId)
        }
    }

    fun getOptimizedImageUrl(
        publicId: String,
        width: Int? = null,
        height: Int? = null,
        crop: String = "fill",
        quality: String = "auto:best"
    ): String {
        val transformations = mutableListOf<String>()
        width?.let { transformations.add("w_$it") }
        height?.let { transformations.add("h_$it") }
        transformations.add("c_$crop")
        transformations.add("q_$quality")
        transformations.add("f_auto")

        val transformationString = transformations.joinToString(",")
        return "https://res.cloudinary.com/$CLOUD_NAME/image/upload/$transformationString/$publicId"
    }
}
