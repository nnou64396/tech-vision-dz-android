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