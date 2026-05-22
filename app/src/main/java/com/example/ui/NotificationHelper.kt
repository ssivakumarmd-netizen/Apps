package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.PriceAlert

object NotificationHelper {
    private const val CHANNEL_ID = "stock_price_alerts_channel"
    private const val CHANNEL_NAME = "Stock Price Alerts"
    private const val CHANNEL_DESC = "Notifications for stock price target alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPriceAlertNotification(context: Context, alert: PriceAlert, currentPrice: Double) {
        // Create an Intent to open MainActivity when clicking the notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val conditionText = if (alert.isAbove) "has gone above" else "has gone below"
        val directionSymbol = if (alert.isAbove) "📈" else "📉"
        
        val title = "$directionSymbol Stock Alert: ${alert.symbol}"
        val content = "${alert.symbol} $conditionText your target of \$${String.format("%.2f", alert.targetPrice)} (Current Price: \$${String.format("%.2f", currentPrice)})!"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat) // Standard Android system icon as fallback
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // check permission before showing (handled in UI, but good to catch security exceptions)
            notificationManager.notify(alert.id, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
