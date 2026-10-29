package com.techvisiondz.app.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun constantsAreDefinedCorrectly() {
        assertEquals("techvision_news_channel", NotificationHelper.CHANNEL_NEWS_ID)
        assertEquals("techvision_announcements_channel", NotificationHelper.CHANNEL_ANNOUNCEMENTS_ID)
        assertEquals("techvision_alerts_channel", NotificationHelper.CHANNEL_ALERTS_ID)
        assertEquals("article_slug", NotificationHelper.EXTRA_ARTICLE_SLUG)
        assertEquals("target_route", NotificationHelper.EXTRA_TARGET_ROUTE)
    }
}
