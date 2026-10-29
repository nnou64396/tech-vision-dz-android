package com.techvisiondz.app.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SourceCategory(val id: String, val titleAr: String, val titleEn: String) {
    ALL("all", "جميع المصادر", "All Sources"),
    SMARTPHONES_ANDROID("smartphones-android", "الهواتف الذكية وأندرويد", "Smartphones & Android"),
    TECHNOLOGY_NEWS("technology-news", "أخبار التكنولوجيا والابتكار الرقمي", "Tech News & Innovation"),
    INTERNATIONAL_NEWS("international-news", "الأخبار والتغطيات الدولية", "International Coverage"),
    COMPUTER_HARDWARE("computer-hardware", "مكونات الحاسوب والرسوميات", "Computer Hardware & PC"),
    SOFTWARE_APPLICATIONS("software-applications", "البرامج والتطبيقات", "Software & Applications");

    companion object {
        fun fromId(id: String): SourceCategory =
            when (id.lowercase().trim()) {
                "software", "software-applications", "applications", "software-apps" -> SOFTWARE_APPLICATIONS
                else -> entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
            }
    }
}

@Serializable
data class Source(
    val id: String,
    val name: String,
    val url: String,
    val descriptionAr: String,
    val descriptionEn: String = descriptionAr,
    val categoryId: String,
    val logoPath: String? = null,
) {
    val logoUrl: String?
        get() = logoPath?.let { path ->
            if (path.startsWith("http")) path else "https://techvisiondz.com$path"
        }

    val category: SourceCategory
        get() = SourceCategory.fromId(categoryId)
}
