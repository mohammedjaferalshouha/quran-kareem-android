package com.quran.labs.androidquran.util

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.quran.labs.androidquran.base.TestApplication
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class QuranSettingsReadingModeTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private lateinit var settings: QuranSettings

  @Before
  fun setUp() {
    PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    QuranSettings.setInstance(null)
    settings = QuranSettings.getInstance(context)
  }

  @After
  fun tearDown() {
    QuranSettings.setInstance(null)
  }

  @Test
  fun `page reader is the default mode`() {
    assertThat(settings.isTextReadingMode).isFalse()
  }

  @Test
  fun `reading mode can switch without forcing text mode`() {
    settings.setTextReadingMode(true)
    assertThat(settings.isTextReadingMode).isTrue()

    settings.setTextReadingMode(false)
    assertThat(settings.isTextReadingMode).isFalse()
  }

  @Test
  fun `automatic reading position remembers its actual mode`() {
    settings.setAutomaticReadingPosition(10, 2, 5, true)
    assertThat(settings.wasAutomaticLastTextMode()).isTrue()

    settings.setAutomaticReadingPosition(11, 2, 6, false)
    assertThat(settings.wasAutomaticLastTextMode()).isFalse()
  }
}
