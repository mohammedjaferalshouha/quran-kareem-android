package com.quran.labs.androidquran.data

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranDisplayDataSearchTest {
  private lateinit var arabicContext: Context
  private lateinit var displayData: QuranDisplayData
  private lateinit var quranInfo: QuranInfo

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(Locale("ar"))
    arabicContext = context.createConfigurationContext(configuration)
    quranInfo = QuranInfo(MadaniDataSource())
    displayData = QuranDisplayData(quranInfo)
  }

  @Test
  fun `search finds normalized Arabic sura names`() {
    assertThat(displayData.findSurasByName(arabicContext, "الكهف").first()).isEqualTo(18)
    assertThat(displayData.findSurasByName(arabicContext, "سورة الكهف").first()).isEqualTo(18)
    assertThat(displayData.findSurasByName(arabicContext, "سُورَة آل عمران").first()).isEqualTo(3)
    assertThat(quranInfo.getPageFromSuraAyah(18, 1)).isEqualTo(293)
  }
}
