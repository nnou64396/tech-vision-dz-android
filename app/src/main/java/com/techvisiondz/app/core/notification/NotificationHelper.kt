package com.techvisiondz.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.techvisiondz.app.MainActivity
import com.techvisiondz.app.R
import com.techvisiondz.app.core.settings.SharedPrefsSettingsPreferences

object NotificationHelper {

    const val CHANNEL_NEWS_ID = "techvision_news_channel"
    const val CHANNEL_ANNOUNCEMENTS_ID = "techvision_announcements_channel"
    const val CHANNEL_ALERTS_ID = "techvision_alerts_channel"

    const val EXTRA_ARTICLE_SLUG = "article_slug"
    const val EXTRA_TARGET_ROUTE = "target_route"

    fun createNotificationChannels(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val newsChannel = NotificationChannel(
            CHANNEL_NEWS_ID,
            context.getString(R.string.notif_channel_news_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_news_desc)
        }

        val announcementsChannel = NotificationChannel(
            CHANNEL_ANNOUNCEMENTS_ID,
            context.getString(R.string.notif_channel_announcements_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_announcements_desc)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS_ID,
            context.getString(R.string.notif_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_alerts_desc)
        }

        notificationManager.createNotificationChannels(
            listOf(newsChannel, announcementsChannel, alertsChannel),
        )
    }

    fun postNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        articleSlug: String? = null,
        targetRoute: String? = null,
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
    ) {
        val prefs = SharedPrefsSettingsPreferences(context)
        if (!prefs.isNotificationsEnabled()) return

        when (channelId) {
            CHANNEL_NEWS_ID -> if (!prefs.isNewsNotificationsEnabled()) return
            CHANNEL_ANNOUNCEMENTS_ID -> if (!prefs.isAnnouncementsNotificationsEnabled()) return
            CHANNEL_ALERTS_ID -> if (!prefs.isAlertsNotificationsEnabled()) return
        }

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!articleSlug.isNullOrBlank()) {
                putExtra(EXTRA_ARTICLE_SLUG, articleSlug)
            }
            if (!targetRoute.isNullOrBlank()) {
                putExtra(EXTRA_TARGET_ROUTE, targetRoute)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.brand_logo_dark)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_ALERTS_ID) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (_: SecurityException) {
            // Ignored if POST_NOTIFICATIONS permission not granted at runtime.
        } catch (_: Exception) {
            // Ignored if device system service fails
        }
    }
}
