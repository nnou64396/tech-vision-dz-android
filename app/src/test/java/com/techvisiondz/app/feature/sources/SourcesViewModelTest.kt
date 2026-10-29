package com.techvisiondz.app.feature.sources

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.Source
import com.techvisiondz.app.core.data.model.SourceCategory
import com.techvisiondz.app.core.data.repository.DefaultSourceRepository
import com.techvisiondz.app.core.data.repository.SourceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesViewModelTest {

    private class FakeSourceRepository : SourceRepository {
        override fun getSources(): List<Source> = listOf(
            Source(
                id = "gsmarena",
                name = "GSMArena",
                url = "https://www.gsmarena.com/",
                descriptionAr = "معلومات ومراجعات وأخبار الهواتف المحمولة.",
                descriptionEn = "Mobile phone specifications and news.",
                categoryId = "smartphones-android",
                logoPath = "/images/sources/gsmarena.ico",
            ),
            Source(
                id = "the-verge",
                name = "The Verge",
                url = "https://www.theverge.com/",
                descriptionAr = "أخبار التكنولوجيا والمنتجات والثقافة الرقمية.",
                descriptionEn = "Technology news and culture.",
                categoryId = "technology-news",
                logoPath = "/images/sources/the-verge.png",
            ),
        )

        override fun getCategories(): List<SourceCategory> = listOf(
            SourceCategory.ALL,
            SourceCategory.SMARTPHONES_ANDROID,
            SourceCategory.TECHNOLOGY_NEWS,
        )
    }

    private class FakeSourceRepositoryWithSoftware : SourceRepository {
        override fun getSources(): List<Source> = listOf(
            Source(
                id = "gsmarena",
                name = "GSMArena",
                url = "https://www.gsmarena.com/",
                descriptionAr = "معلومات ومراجعات وأخبار الهواتف المحمولة.",
                descriptionEn = "Mobile phone specifications and news.",
                categoryId = "smartphones-android",
            ),
            Source(
                id = "software-tool",
                name = "Software Download Tool",
                url = "https://example.com/tool",
                descriptionAr = "أداة تنزيل البرامج.",
                descriptionEn = "Software download tool.",
                categoryId = "software-applications",
            ),
        )

        override fun getCategories(): List<SourceCategory> = listOf(
            SourceCategory.ALL,
            SourceCategory.SMARTPHONES_ANDROID,
            SourceCategory.SOFTWARE_APPLICATIONS,
        )
    }

    @Test
    fun loadsAllSourcesInitially() {
        val viewModel = SourcesViewModel(FakeSourceRepository())

        assertEquals(2, viewModel.uiState.value.sources.size)
        assertEquals(2, viewModel.uiState.value.filteredSources.size)
    }

    @Test
    fun filtersSourcesByCategory() {
        val viewModel = SourcesViewModel(FakeSourceRepository())

        viewModel.onCategorySelected(SourceCategory.SMARTPHONES_ANDROID)

        assertEquals(1, viewModel.uiState.value.filteredSources.size)
        assertEquals("GSMArena", viewModel.uiState.value.filteredSources.first().name)
    }

    @Test
    fun filtersSourcesBySearchQuery() {
        val viewModel = SourcesViewModel(FakeSourceRepository())

        viewModel.onSearchQueryChanged("Verge")

        assertEquals(1, viewModel.uiState.value.filteredSources.size)
        assertEquals("The Verge", viewModel.uiState.value.filteredSources.first().name)
    }

    @Test
    fun returnsEmptyListWhenQueryDoesNotMatch() {
        val viewModel = SourcesViewModel(FakeSourceRepository())

        viewModel.onSearchQueryChanged("NonExistentSource12345")

        assertTrue(viewModel.uiState.value.filteredSources.isEmpty())
    }

    @Test
    fun hidesSoftwareApplicationsCategorySources() {
        val viewModel = SourcesViewModel(FakeSourceRepositoryWithSoftware())

        assertEquals(1, viewModel.uiState.value.sources.size)
        assertEquals("GSMArena", viewModel.uiState.value.sources.first().name)
        assertTrue(viewModel.uiState.value.sources.none { it.categoryId == "software-applications" })
    }

    @Test
    fun bundledFallbackDataExcludesSoftwareCategory() {
        val repository = DefaultSourceRepository()
        val sources = repository.getSources()
        assertTrue(sources.none { it.categoryId == "software-applications" || it.categoryId == "software" })
    }

    @Test
    fun searchIgnoresSoftwareCategorySources() {
        val viewModel = SourcesViewModel(FakeSourceRepositoryWithSoftware())

        viewModel.onSearchQueryChanged("Software Download")
        assertTrue(viewModel.uiState.value.filteredSources.isEmpty())
    }

    @Test
    fun selectingHiddenCategoryFallsBackSafelyToAll() {
        val viewModel = SourcesViewModel(FakeSourceRepositoryWithSoftware())

        viewModel.onCategorySelected(SourceCategory.SOFTWARE_APPLICATIONS)

        assertEquals(SourceCategory.ALL, viewModel.uiState.value.selectedCategory)
        assertEquals(1, viewModel.uiState.value.filteredSources.size)
    }

    @Test
    fun verifySourcesManifestUrlIsUnchanged() {
        assertEquals("https://techvisiondz.com/sources.json", AppConfig.SOURCES_MANIFEST_URL)
    }
}
