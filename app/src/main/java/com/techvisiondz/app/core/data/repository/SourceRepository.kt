package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.Source
import com.techvisiondz.app.core.data.model.SourceCategory

interface SourceRepository {
    fun getSources(): List<Source>
    fun getCategories(): List<SourceCategory>
}

/**
 * Repository for the official Tech Vision DZ technology sources directory.
 * Matches https://techvisiondz.com/sources exactly (28 sources across 4 domains).
 */
class DefaultSourceRepository : SourceRepository {

    private val officialSources = listOf(
        // Smartphones & Android (الهواتف الذكية وأندرويد)
        Source(
            id = "gsmarena",
            name = "GSMArena",
            url = "https://www.gsmarena.com/",
            descriptionAr = "معلومات ومراجعات وأخبار الهواتف المحمولة.",
            descriptionEn = "Mobile phone specifications, news, and in-depth reviews.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/gsmarena.ico",
        ),
        Source(
            id = "android-authority",
            name = "Android Authority",
            url = "https://www.androidauthority.com/",
            descriptionAr = "أخبار أندرويد والهواتف والتطبيقات والأجهزة الذكية.",
            descriptionEn = "Android news, smartphone reviews, apps, and smart devices.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/android-authority.ico",
        ),
        Source(
            id = "9to5google",
            name = "9to5Google",
            url = "https://9to5google.com/",
            descriptionAr = "تغطية أخبار Google وأندرويد والمنتجات والخدمات المرتبطة بها.",
            descriptionEn = "Comprehensive coverage of Google, Android, Pixel, and ecosystem services.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/9to5google.png",
        ),
        Source(
            id = "sammobile",
            name = "SamMobile",
            url = "https://www.sammobile.com/",
            descriptionAr = "أخبار أجهزة Samsung وبرامجها وتحديثاتها.",
            descriptionEn = "Samsung Galaxy news, One UI software updates, and firmware.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/sammobile.png",
        ),
        Source(
            id = "frandroid",
            name = "Frandroid",
            url = "https://www.frandroid.com/",
            descriptionAr = "موقع فرنسي متخصص في أخبار التقنية ومراجعات الهواتف والأجهزة وأدلة الشراء.",
            descriptionEn = "French technology media covering smartphone reviews, buying guides, and tech news.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/frandroid.png",
        ),
        Source(
            id = "ximitime",
            name = "XimiTime",
            url = "https://ximitime.com/",
            descriptionAr = "أخبار هواتف Xiaomi وتحديثات HyperOS والمراجعات والتسريبات.",
            descriptionEn = "Xiaomi smartphone news, HyperOS updates, leaks, and reviews.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/ximitime.png",
        ),
        Source(
            id = "huawei-central",
            name = "Huawei Central",
            url = "https://www.huaweicentral.com/",
            descriptionAr = "أخبار Huawei وأجهزتها وتحديثات HarmonyOS والمراجعات.",
            descriptionEn = "Huawei devices, HarmonyOS updates, and ecosystem news.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/huawei-central.ico",
        ),
        Source(
            id = "android-update-tracker",
            name = "Android Update Tracker",
            url = "https://www.androidupdatetracker.com/",
            descriptionAr = "متابعة أحدث إصدارات أندرويد المتاحة للهواتف ومواعيد وصول التحديثات.",
            descriptionEn = "Track official Android OS updates and device rollout schedules.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/android-update-tracker.png",
        ),
        Source(
            id = "saudi-android",
            name = "سعودي اندرويد",
            url = "https://www.saudiandroid.net/",
            descriptionAr = "أخبار أندرويد والهواتف والتطبيقات والأنظمة باللغة العربية.",
            descriptionEn = "Arabic news coverage for Android OS, smartphones, and mobile apps.",
            categoryId = "smartphones-android",
            logoPath = "/images/sources/saudi-android.jpg",
        ),

        // Tech News & Innovation (أخبار التكنولوجيا والابتكار الرقمي)
        Source(
            id = "the-verge",
            name = "The Verge",
            url = "https://www.theverge.com/",
            descriptionAr = "أخبار التكنولوجيا والمنتجات والثقافة الرقمية.",
            descriptionEn = "Technology news, consumer products, and digital culture.",
            categoryId = "technology-news",
            logoPath = "/images/sources/the-verge.png",
        ),
        Source(
            id = "techcrunch",
            name = "TechCrunch",
            url = "https://techcrunch.com/",
            descriptionAr = "أخبار شركات التقنية والشركات الناشئة والابتكار.",
            descriptionEn = "Tech startups, venture capital, and digital innovation news.",
            categoryId = "technology-news",
            logoPath = "/images/sources/techcrunch.webp",
        ),
        Source(
            id = "engadget",
            name = "Engadget",
            url = "https://www.engadget.com/",
            descriptionAr = "أخبار الأجهزة والإلكترونيات الاستهلاكية والتقنية.",
            descriptionEn = "Consumer electronics news, gadget reviews, and personal tech.",
            categoryId = "technology-news",
            logoPath = "/images/sources/engadget.png",
        ),
        Source(
            id = "wired",
            name = "WIRED",
            url = "https://www.wired.com/",
            descriptionAr = "تغطية التكنولوجيا والعلوم وتأثيرهما في المجتمع.",
            descriptionEn = "Deep reporting on technology, science, and digital culture.",
            categoryId = "technology-news",
            logoPath = "/images/sources/wired.ico",
        ),
        Source(
            id = "comss",
            name = "COMSS",
            url = "https://www.comss.ru/",
            descriptionAr = "أخبار برامج الحماية وأمن المعلومات وتحديثات Windows وأندرويد.",
            descriptionEn = "Cybersecurity news, antivirus software, Windows, and Android updates.",
            categoryId = "technology-news",
            logoPath = "/images/sources/comss.png",
        ),
        Source(
            id = "tunecom",
            name = "Tunecom",
            url = "https://tunecom.ru/",
            descriptionAr = "شروحات وأخبار Windows وبرامج الحاسوب والأمن الرقمي ومكونات الأجهزة.",
            descriptionEn = "Windows tutorials, software security, digital tools, and PC hardware.",
            categoryId = "technology-news",
            logoPath = "/images/sources/tunecom.png",
        ),
        Source(
            id = "aqweeb",
            name = "أكوا ويب",
            url = "https://aqweeb.com/",
            descriptionAr = "موقع تقني عربي يغطي الذكاء الاصطناعي والبرمجة والتطبيقات والألعاب والمراجعات.",
            descriptionEn = "Arabic technology portal covering AI, programming, apps, and gaming.",
            categoryId = "technology-news",
            logoPath = "/images/sources/aqweeb.png",
        ),
        Source(
            id = "ait-news",
            name = "البوابة التقنية",
            url = "https://aitnews.com/",
            descriptionAr = "أخبار ومستجدات التقنية والشركات والمنتجات باللغة العربية.",
            descriptionEn = "Arab IT News portal covering tech industry, companies, and product releases.",
            categoryId = "technology-news",
            logoPath = "/images/sources/ait-news.png",
        ),
        Source(
            id = "tech-mag",
            name = "مجلة عالم التكنولوجيا",
            url = "https://www.tech-mag.net/",
            descriptionAr = "مجلة عربية شاملة تغطي أخبار التكنولوجيا والأجهزة والتطبيقات.",
            descriptionEn = "Comprehensive Arabic magazine covering technology, devices, and software.",
            categoryId = "technology-news",
            logoPath = "/images/sources/tech-mag.jpg",
        ),
        Source(
            id = "macrumors",
            name = "MacRumors",
            url = "https://www.macrumors.com/",
            descriptionAr = "أخبار Apple وتسريبات منتجاتها وتحديثات أنظمتها.",
            descriptionEn = "Apple news, Mac and iPhone rumors, and iOS updates.",
            categoryId = "technology-news",
            logoPath = "/images/sources/macrumors.png",
        ),
        Source(
            id = "9to5mac",
            name = "9to5Mac",
            url = "https://9to5mac.com/",
            descriptionAr = "تغطية أخبار Apple وأجهزة Mac وiPhone والبرامج والخدمات.",
            descriptionEn = "Apple ecosystem news, hardware reviews, and macOS/iOS updates.",
            categoryId = "technology-news",
            logoPath = "/images/sources/9to5mac.png",
        ),

        // International News & Coverage (الأخبار والتغطيات الدولية)
        Source(
            id = "reuters",
            name = "Reuters",
            url = "https://www.reuters.com/",
            descriptionAr = "تغطية إخبارية دولية تشمل الشركات والاقتصاد والتكنولوجيا.",
            descriptionEn = "Global news coverage spanning technology, business, and markets.",
            categoryId = "international-news",
            logoPath = "/images/sources/reuters.ico",
        ),
        Source(
            id = "bloomberg",
            name = "Bloomberg",
            url = "https://www.bloomberg.com/",
            descriptionAr = "أخبار الأعمال والأسواق والشركات وقطاع التكنولوجيا.",
            descriptionEn = "Business, financial markets, and tech sector reporting.",
            categoryId = "international-news",
            logoPath = "/images/sources/bloomberg.ico",
        ),
        Source(
            id = "associated-press",
            name = "Associated Press",
            url = "https://apnews.com/",
            descriptionAr = "تغطية إخبارية دولية وتقارير عن مستجدات التقنية.",
            descriptionEn = "International news wire covering technology developments and global policy.",
            categoryId = "international-news",
            logoPath = "/images/sources/associated-press.png",
        ),

        // Computer Hardware & PC (مكونات الحاسوب والرسوميات)
        Source(
            id = "techpowerup",
            name = "TechPowerUp",
            url = "https://www.techpowerup.com/",
            descriptionAr = "أخبار ومراجعات بطاقات الرسوميات ومكونات الحاسوب.",
            descriptionEn = "Graphics card reviews, PC hardware news, and GPU databases.",
            categoryId = "computer-hardware",
            logoPath = "/images/sources/techpowerup.ico",
        ),
        Source(
            id = "toms-hardware",
            name = "Tom's Hardware",
            url = "https://www.tomshardware.com/",
            descriptionAr = "أخبار ومراجعات الحواسيب والمعالجات ومكونات العتاد.",
            descriptionEn = "PC hardware reviews, CPU/GPU benchmarks, and building guides.",
            categoryId = "computer-hardware",
            logoPath = "/images/sources/toms-hardware.ico",
        ),
        Source(
            id = "videocardz",
            name = "VideoCardz",
            url = "https://videocardz.com/",
            descriptionAr = "أخبار بطاقات الرسوميات ومعالجاتها وتقنيات العرض.",
            descriptionEn = "Graphics card news, GPU leaks, and display technology.",
            categoryId = "computer-hardware",
            logoPath = "/images/sources/videocardz.png",
        ),
        Source(
            id = "wccftech",
            name = "Wccftech",
            url = "https://wccftech.com/",
            descriptionAr = "أخبار ومراجعات العتاد والمعالجات والرسوميات والألعاب.",
            descriptionEn = "PC hardware, CPU/GPU leaks, mobile hardware, and gaming news.",
            categoryId = "computer-hardware",
            logoPath = "/images/sources/wccftech.png",
        ),
        Source(
            id = "hothardware",
            name = "HotHardware",
            url = "https://hothardware.com/",
            descriptionAr = "أخبار ومراجعات الحواسيب والأجهزة الاستهلاكية وتقنيات الحوسبة.",
            descriptionEn = "Computing technology, PC hardware reviews, and consumer tech.",
            categoryId = "computer-hardware",
            logoPath = "/images/sources/hothardware.png",
        ),
    )

    override fun getSources(): List<Source> =
        officialSources.filter { it.category != SourceCategory.SOFTWARE_APPLICATIONS }

    override fun getCategories(): List<SourceCategory> = listOf(
        SourceCategory.ALL,
        SourceCategory.SMARTPHONES_ANDROID,
        SourceCategory.TECHNOLOGY_NEWS,
        SourceCategory.INTERNATIONAL_NEWS,
        SourceCategory.COMPUTER_HARDWARE,
    )
}
