package org.example.project.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

actual object Notifier {
    private var appContext: Context? = null
    private const val CHANNEL_ID = "extingrafic_alerts"

    actual fun init(context: Any?) {
        appContext = (context as? Context)?.applicationContext ?: appContext
        createChannel()
    }

    private fun createChannel() {
        val ctx = appContext ?: return
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas ExtinGrafic",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    actual fun notify(title: String, message: String) {
        val ctx = appContext ?: return
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        NotificationManagerCompat.from(ctx).notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
