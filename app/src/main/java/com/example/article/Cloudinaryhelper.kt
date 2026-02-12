package com.example.article.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CloudinaryHelper {

    private const val TAG = "CloudinaryHelper"

    // 🔥 Replace with YOUR values
    private const val CLOUD_NAME = "dpn1202gn"
    private const val UPLOAD_PRESET = "dpn1202gn_preset"

    private const val UPLOAD_URL =
        "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val client = OkHttpClient()

    suspend fun uploadImage(
        imageUri: Uri,
        context: Context,
        folder: String
    ): Result<CloudinaryResult> = withContext(Dispatchers.IO) {
        try {
            val file = uriToFile(imageUri, context)
                ?: return@withContext Result.failure(Exception("File conversion failed"))

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("folder", folder)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Upload failed: ${response.message}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val json = JSONObject(responseBody)
            val secureUrl = json.getString("secure_url")
            val publicId = json.getString("public_id")

            file.delete()

            Result.success(CloudinaryResult(secureUrl, publicId))

        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            Result.failure(e)
        }
    }

    fun isConfigured(): Boolean {
        return CLOUD_NAME.isNotBlank() && UPLOAD_PRESET.isNotBlank()
    }

    private fun uriToFile(uri: Uri, context: Context): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            val tempFile = File.createTempFile(
                "upload_${System.currentTimeMillis()}",
                ".jpg",
                context.cacheDir
            )

            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            null
        }
    }
}

data class CloudinaryResult(
    val secureUrl: String,
    val publicId: String
)
