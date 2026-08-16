package com.quran.labs.androidquran.ui

import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.pages.data.madani.Madani1441DataSource
import org.junit.Test

class PagerActivityReadingAnchorTest {

  private val quranInfo = QuranInfo(Madani1441DataSource())

  @Test
  fun `reported visible ayah wins during a mode switch`() {
    val reported = SuraAyah(2, 199)
    val page = quranInfo.getPageFromSuraAyah(reported.sura, reported.ayah)

    val anchor = PagerActivity.resolveReadingAnchor(
      quranInfo,
      page,
      reported,
      SuraAyah(2, 197)
    )

    assertThat(anchor).isEqualTo(reported)
  }

  @Test
  fun `retained visible ayah survives page and text mode recreation`() {
    val retained = SuraAyah(22, 3)
    val page = quranInfo.getPageFromSuraAyah(retained.sura, retained.ayah)

    val anchor = PagerActivity.resolveReadingAnchor(quranInfo, page, null, retained)

    assertThat(anchor).isEqualTo(retained)
  }

  @Test
  fun `anchor from another page falls back to first ayah on current page`() {
    val page = 332
    val bounds = quranInfo.getPageBounds(page)

    val anchor = PagerActivity.resolveReadingAnchor(
      quranInfo,
      page,
      SuraAyah(2, 199),
      SuraAyah(21, 102)
    )

    assertThat(anchor).isEqualTo(SuraAyah(bounds[0], bounds[1]))
  }
}
