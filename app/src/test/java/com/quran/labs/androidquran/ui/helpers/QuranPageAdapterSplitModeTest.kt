package com.quran.labs.androidquran.ui.helpers

import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.base.TestApplication
import com.quran.labs.androidquran.pages.data.madani.Madani1441DataSource
import com.quran.labs.androidquran.ui.fragment.TabletFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class QuranPageAdapterSplitModeTest {

  private val quranInfo = QuranInfo(Madani1441DataSource())

  @Test
  fun `split page mode keeps the mushaf reader on the right`() {
    val adapter = QuranPageAdapter(
      null,
      isDualPages = true,
      isShowingTranslation = false,
      quranInfo = quranInfo,
      isSplitScreen = true
    )

    val fragment = adapter.getItem(0) as TabletFragment

    assertThat(fragment.requireArguments().getBoolean("splitScreenMode")).isTrue()
    assertThat(fragment.requireArguments().getBoolean("splitReaderIsText")).isFalse()
    assertThat(adapter.count).isEqualTo(quranInfo.numberOfPagesConsideringSkipped)
  }

  @Test
  fun `split text mode keeps flowing Quran text on the right`() {
    val adapter = QuranPageAdapter(
      null,
      isDualPages = true,
      isShowingTranslation = true,
      quranInfo = quranInfo,
      isSplitScreen = true
    )

    val fragment = adapter.getItem(0) as TabletFragment

    assertThat(fragment.requireArguments().getBoolean("splitScreenMode")).isTrue()
    assertThat(fragment.requireArguments().getBoolean("splitReaderIsText")).isTrue()
    assertThat(adapter.count).isEqualTo(quranInfo.numberOfPagesConsideringSkipped)
  }
}
