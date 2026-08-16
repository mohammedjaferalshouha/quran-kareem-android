package com.quran.data.page.provider.madani

import com.quran.data.source.DisplaySize
import com.quran.data.source.PageContentType
import com.quran.data.source.PageProvider
import com.quran.data.source.PageSizeCalculator
import com.quran.labs.androidquran.pages.data.madani.Madani1441DataSource
import com.quran.labs.androidquran.pages.madani.R

class Madani1441LinePageProvider(
  private val delegate: PageProvider = MadaniPageProvider()
) : PageProvider by delegate {

  override fun getDataSource() = dataSource

  override fun getPageSizeCalculator(displaySize: DisplaySize): PageSizeCalculator =
    FixedPageSizeCalculator(PAGE_WIDTH)

  override fun getImageVersion() = 1

  override fun getImagesBaseUrl() = "$BASE_URL/"

  override fun getImagesZipBaseUrl() = "$BASE_URL/zips/"

  override fun getPatchBaseUrl() = "$BASE_URL/patches/v"

  override fun getAyahInfoBaseUrl() = "$BASE_URL/databases/ayahinfo/"

  override fun getAudioDirectoryName() = AUDIO_DIRECTORY

  override fun getDatabaseDirectoryName() = "$DATA_DIRECTORY/databases"

  override fun getAyahInfoDirectoryName() = getDatabaseDirectoryName()

  override fun getDatabasesBaseUrl() = SHARED_DATABASES_BASE_URL

  override fun getAudioDatabasesBaseUrl() = "${SHARED_DATABASES_BASE_URL}audio/"

  override fun getImagesDirectoryName() = DATA_DIRECTORY

  override fun getPreviewTitle() = R.string.madani_1441_lines_title

  override fun getPreviewDescription() = R.string.madani_1441_lines_description

  override fun getPageContentType() = PageContentType.Line(
    ratio = 0.16111112f,
    lineHeight = 174,
    allowOverlapOfLines = true
  )

  override fun getFallbackPageType() = FALLBACK_PAGE_TYPE

  override fun pageType() = PAGE_TYPE

  private class FixedPageSizeCalculator(private val width: String) : PageSizeCalculator {
    override fun getWidthParameter() = width

    override fun getTabletWidthParameter() = width

    override fun setOverrideParameter(parameter: String) = Unit
  }

  companion object {
    const val PAGE_TYPE = "madani_1441_lines"
    const val FALLBACK_PAGE_TYPE = "madani"
    const val PAGE_WIDTH = "1440"
    const val DATA_DIRECTORY = "new_madani"
    // Reciter files are ayah-based and compatible with the classic Madani reader.
    // Keep the shared directory so switching page renderers never hides downloads.
    const val AUDIO_DIRECTORY = "audio"
    const val BASE_URL = "https://files.quran.app/hafs/madani_1441"
    const val SHARED_DATABASES_BASE_URL = "https://files.quran.app/hafs/databases/"

    private val dataSource by lazy { Madani1441DataSource() }
  }
}
