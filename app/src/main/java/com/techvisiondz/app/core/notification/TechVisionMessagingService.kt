package com.techvisiondz.app.core.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.techvisiondz.app.core.settings.SharedPrefsSettingsPreferences

class TechVisionMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SharedPrefsSettingsPreferences(applicationContext).setFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: return
        val body = notification?.body ?: data["body"] ?: return
        val channelId = data["channel_id"]
            ?: notification?.channelId
            ?: NotificationHelper.CHANNEL_NEWS_ID

        val articleSlug = data["article_slug"] ?: data["slug"]
        val targetRoute = data["target_route"] ?: data["route"]

        NotificationHelper.postNotification(
            context = applicationContext,
            channelId = channelId,
            title = title,
            body = body,
            articleSlug = articleSlug,
            targetRoute = targetRoute,
        )
    }
}
