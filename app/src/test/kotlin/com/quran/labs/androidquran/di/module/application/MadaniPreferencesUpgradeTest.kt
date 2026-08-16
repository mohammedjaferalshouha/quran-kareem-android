package com.quran.labs.androidquran.di.module.application

import android.app.Application
import android.preference.PreferenceManager
import com.google.common.truth.Truth.assertThat
import com.quran.labs.androidquran.data.Constants
import com.quran.data.dao.Settings
import com.quran.labs.androidquran.pages.common.madani.upgrade.MadaniPreferencesUpgrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class MadaniPreferencesUpgradeTest {

  @Suppress("DEPRECATION")
  @Test
  fun `upgrade to 10100 preserves an existing page and reading mode choice`() {
    val context = RuntimeEnvironment.getApplication()
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    preferences.edit()
      .clear()
      .putString(Constants.PREF_PAGE_TYPE, "madani")
      .putBoolean(Constants.PREF_READING_TEXT_MODE, true)
      .commit()

    MadaniPreferencesUpgrade(NoopSettings()).upgrade(context, 10000, 10100)

    assertThat(preferences.getString(Constants.PREF_PAGE_TYPE, null))
      .isEqualTo("madani")
    assertThat(preferences.getBoolean(Constants.PREF_READING_TEXT_MODE, false))
      .isTrue()
  }

  @Suppress("DEPRECATION")
  @Test
  fun `upgrade to 10100 leaves absent reading choices to application defaults`() {
    val context = RuntimeEnvironment.getApplication()
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    preferences.edit().clear().commit()

    MadaniPreferencesUpgrade(NoopSettings()).upgrade(context, 0, 10100)

    assertThat(preferences.contains(Constants.PREF_PAGE_TYPE)).isFalse()
    assertThat(preferences.contains(Constants.PREF_READING_TEXT_MODE)).isFalse()
  }

  @Suppress("DEPRECATION")
  @Test
  fun `upgrade after 10100 preserves the selected page and reading mode`() {
    val context = RuntimeEnvironment.getApplication()
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    preferences.edit()
      .clear()
      .putString(Constants.PREF_PAGE_TYPE, "madani")
      .putBoolean(Constants.PREF_READING_TEXT_MODE, true)
      .commit()

    MadaniPreferencesUpgrade(NoopSettings()).upgrade(context, 10100, 10101)

    assertThat(preferences.getString(Constants.PREF_PAGE_TYPE, null))
      .isEqualTo("madani")
    assertThat(preferences.getBoolean(Constants.PREF_READING_TEXT_MODE, false))
      .isTrue()
  }

  private class NoopSettings : Settings {
    override suspend fun setVersion(version: Int) = Unit
    override suspend fun setShouldOverlayPageInfo(shouldOverlay: Boolean) = Unit
    override suspend fun lastPage() = 1
    override suspend fun isNightMode() = false
    override suspend fun nightModeTextBrightness() = 255
    override suspend fun nightModeBackgroundBrightness() = 0
    override suspend fun shouldShowHeaderFooter() = true
    override suspend fun shouldShowBookmarks() = true
    override suspend fun pageType() = "madani"
    override suspend fun setPageType(pageType: String) = Unit
    override suspend fun showSidelines() = false
    override suspend fun setShowSidelines(show: Boolean) = Unit
    override suspend fun showLineDividers() = false
    override suspend fun setShouldShowLineDividers(show: Boolean) = Unit
    override suspend fun setAyahTextSize(value: Int) = Unit
    override suspend fun translationTextSize() = 15
    override fun preferencesFlow(): Flow<String> = emptyFlow()
  }
}
