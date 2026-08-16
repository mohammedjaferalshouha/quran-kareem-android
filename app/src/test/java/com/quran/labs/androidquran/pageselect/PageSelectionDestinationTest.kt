package com.quran.labs.androidquran.pageselect

import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.pages.data.madani.Madani1441DataSource
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import org.junit.Test

class PageSelectionDestinationTest {

  private val quranInfo = QuranInfo(Madani1441DataSource())

  @Test
  fun `maps the saved ayah through the selected page type`() {
    val destination = resolvePageSelectionDestination(quranInfo, 50, 2, 255)

    assertThat(destination.page).isEqualTo(quranInfo.getPageFromSuraAyah(2, 255))
    assertThat(destination.sura).isEqualTo(2)
    assertThat(destination.ayah).isEqualTo(255)
  }

  @Test
  fun `uses a valid saved page when the ayah is invalid`() {
    val destination = resolvePageSelectionDestination(quranInfo, 50, -1, -1)

    assertThat(destination.page).isEqualTo(50)
    assertThat(destination.sura).isNull()
    assertThat(destination.ayah).isNull()
  }

  @Test
  fun `uses first page when all saved coordinates are invalid`() {
    val destination = resolvePageSelectionDestination(quranInfo, -1, -1, -1)

    assertThat(destination.page).isEqualTo(1)
  }

  @Test
  fun `derives an ayah from the old page when no ayah was recorded`() {
    val oldQuranInfo = QuranInfo(MadaniDataSource())
    val oldPage = 50

    val source = resolveSourceReadingPosition(oldQuranInfo, oldPage, -1, -1)
    val destination = resolvePageSelectionDestination(
      quranInfo,
      oldPage,
      source.sura,
      source.ayah
    )

    val oldBounds = oldQuranInfo.getPageBounds(oldPage)
    assertThat(source).isEqualTo(SourceReadingPosition(oldBounds[0], oldBounds[1]))
    assertThat(destination.page)
      .isEqualTo(quranInfo.getPageFromSuraAyah(source.sura, source.ayah))
  }

  @Test
  fun `prefers the exact recorded ayah over the page fallback`() {
    val source = resolveSourceReadingPosition(quranInfo, 50, 2, 255)

    assertThat(source).isEqualTo(SourceReadingPosition(2, 255))
  }
}
