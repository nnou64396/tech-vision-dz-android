package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.data.model.ArticleCard

/** Content model for the home screen. */
data class HomeContent(
    val articles: List<ArticleCard> = emptyList(),
)