package com.techvisiondz.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Branded supplementary icon set.
 *
 * The app depends on the small `material-icons-core` artifact, which does not
 * ship a bookmark glyph. Keeping two hand-built vectors avoids pulling in the
 * multi-megabyte `material-icons-extended` artifact; the path data is the
 * official Material Design `bookmark` / `bookmark_border` 24dp glyph.
 */
object TechVisionIcons {

    /** Filled bookmark — shown when an article is saved. */
    val Bookmark: ImageVector by lazy {
        icon(
            name = "TechVisionBookmark",
            pathData = "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z",
        )
    }

    /** Outline bookmark — shown when an article is not saved yet. */
    val BookmarkBorder: ImageVector by lazy {
        icon(
            name = "TechVisionBookmarkBorder",
            pathData = "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z",
        )
    }

    /** Update glyph — used by the account "Check for updates" row. */
    val Update: ImageVector by lazy {
        icon(
            name = "TechVisionUpdate",
            pathData = "M21 10.12h-6.78l2.74-2.82c-2.73-2.7-7.15-2.8-9.88-.1-2.73 2.71-2.73 " +
                "7.08 0 9.79s7.15 2.71 9.88 0C18.32 15.65 19 14.08 19 12.1h2c0 1.98-.88 " +
                "4.55-2.64 6.29-3.51 3.48-9.21 3.48-12.72 0-3.5-3.47-3.53-9.11-.02-12.58s9.14-3.47 " +
                "12.65 0L21 3v7.12zM12.5 8v4.25l3.5 2.08-.72 1.21L11 13V8h1.5z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black),
        ).build()
}