package com.techvisiondz.app.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 *
 * Only routes that are actually implemented are listed. Future screens
 * (categories, search, favorites, profile, auth, settings, ...) will be added
 * here as they are built - never defined ahead of implementation.
 */
object Routes {
    @Serializable
    data object Home

    @Serializable
    data class ArticleDetail(val slug: String)
}