package com.example.article.Repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.article.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ArticleFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ArticleFCM"
        const val CHANNEL_ID = "article_notifications"
        const val CHANNEL_NAME = "Article Notifications"

        fun saveTokenIfLoggedIn() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> saveTokenToFirestore(uid, token) }
                .addOnFailureListener { e -> Log.e(TAG, "Failed to get FCM token", e) }
        }

        fun saveTokenToFirestore(uid: String, token: String) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnFailureListener { e -> Log.e(TAG, "Failed to save FCM token", e) }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveTokenToFirestore(uid, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Article"
        val body  = remoteMessage.notification?.body  ?: remoteMessage.data["body"]  ?: ""
        val type  = remoteMessage.data["type"] ?: ""
        val refId = remoteMessage.data["referenceId"] ?: ""
        showLocalNotification(title, body, type, refId)
    }

    private fun showLocalNotification(
        title: String, body: String, type: String, referenceId: String
    ) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Neighbourhood updates, messages, and service requests"
                enableLights(true)
                enableVibration(true)
            }
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("notification_type", type)
                putExtra("reference_id", referenceId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = when (type) {
            AppNotification.TYPE_ANNOUNCEMENT    -> android.R.drawable.ic_menu_info_details
            AppNotification.TYPE_MESSAGE         -> android.R.drawable.ic_dialog_email
            AppNotification.TYPE_SERVICE_REQUEST -> android.R.drawable.ic_menu_manage
            else                                 -> android.R.drawable.ic_dialog_info
        }

        nm.notify(
            System.currentTimeMillis().toInt(),
            androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        )
    }
}