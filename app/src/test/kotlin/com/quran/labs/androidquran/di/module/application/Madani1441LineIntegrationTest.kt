package com.quran.labs.androidquran.di.module.application

import com.google.common.truth.Truth.assertThat
import com.quran.data.page.provider.madani.Madani1441LinePageProvider
import com.quran.data.source.DisplaySize
import com.quran.data.source.PageContentType
import com.quran.labs.androidquran.extra.feature.linebyline.QuranLineByLinePageViewFactory
import org.junit.Test

class Madani1441LineIntegrationTest {

  @Test
  fun `provider uses official 1441 line configuration`() {
    val provider = Madani1441LinePageProvider()

    assertThat(provider.pageType()).isEqualTo("madani_1441_lines")
    assertThat(provider.getFallbackPageType()).isEqualTo("madani")
    assertThat(provider.getImagesBaseUrl())
      .isEqualTo("https://files.quran.app/hafs/madani_1441/")
    assertThat(provider.getImagesZipBaseUrl())
      .isEqualTo("https://files.quran.app/hafs/madani_1441/zips/")
    assertThat(provider.getAyahInfoBaseUrl())
      .isEqualTo("https://files.quran.app/hafs/madani_1441/databases/ayahinfo/")
    assertThat(provider.getImagesDirectoryName()).isEqualTo("new_madani")
    assertThat(provider.getDatabaseDirectoryName()).isEqualTo("new_madani/databases")
    assertThat(provider.getAudioDirectoryName()).isEqualTo("audio")
    assertThat(provider.getDatabasesBaseUrl())
      .isEqualTo("https://files.quran.app/hafs/databases/")
    assertThat(provider.getAudioDatabasesBaseUrl())
      .isEqualTo("https://files.quran.app/hafs/databases/audio/")
    assertThat(provider.getPageContentType()).isEqualTo(
      PageContentType.Line(
        ratio = 0.16111112f,
        lineHeight = 174,
        allowOverlapOfLines = true
      )
    )
  }

  @Test
  fun `provider always requests the official 1440 assets`() {
    val calculator = Madani1441LinePageProvider()
      .getPageSizeCalculator(DisplaySize(320, 480))

    assertThat(calculator.getWidthParameter()).isEqualTo("1440")
    assertThat(calculator.getTabletWidthParameter()).isEqualTo("1440")

    calculator.setOverrideParameter("1024")

    assertThat(calculator.getWidthParameter()).isEqualTo("1440")
    assertThat(calculator.getTabletWidthParameter()).isEqualTo("1440")
  }

  @Test
  fun `provider exposes complete 1441 page mappings`() {
    val dataSource = Madani1441LinePageProvider().getDataSource()

    assertThat(dataSource.numberOfPages).isEqualTo(604)
    assertThat(dataSource.suraForPageArray).hasLength(604)
    assertThat(dataSource.ayahForPageArray).hasLength(604)
    assertThat(dataSource.suraForPageArray[603]).isEqualTo(112)
    assertThat(dataSource.ayahForPageArray[603]).isEqualTo(1)
  }

  @Test
  fun `factory provider only replaces the 1441 line page type`() {
    val provider = ApplicationModule.createPageViewFactoryProvider { false }

    assertThat(provider.providePageViewFactory("madani_1441_lines"))
      .isInstanceOf(QuranLineByLinePageViewFactory::class.java)
    assertThat(provider.providePageViewFactory("madani")).isNull()
  }

}
