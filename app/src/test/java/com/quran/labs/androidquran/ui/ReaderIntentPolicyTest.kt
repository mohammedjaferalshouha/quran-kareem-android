package com.quran.labs.androidquran.ui

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.quran.labs.androidquran.QuranDataActivity
import com.quran.labs.androidquran.explicitReaderMode
import com.quran.labs.androidquran.prepareForReaderReuse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReaderIntentPolicyTest {

  @Test
  fun `reader reuse policy sets task flags and explicit image mode`() {
    val intent = Intent().prepareForReaderReuse(jumpToTranslation = false)

    assertThat(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP).isNotEqualTo(0)
    assertThat(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP).isNotEqualTo(0)
    assertThat(intent.hasExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION)).isTrue()
    assertThat(intent.getBooleanExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, true)).isFalse()
    assertThat(intent.explicitReaderMode()).isFalse()
  }

  @Test
  fun `reader mode is absent unless the caller explicitly chooses it`() {
    assertThat(Intent().explicitReaderMode()).isNull()

    val textIntent = Intent().putExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, true)
    assertThat(textIntent.explicitReaderMode()).isTrue()
  }

  @Test
  fun `data readiness route preserves explicit image mode`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val intent = QuranDataActivity.openPageIntent(
      context = context,
      page = 2,
      sura = 2,
      ayah = 1,
      jumpToTranslation = false
    )

    assertThat(intent.hasExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION)).isTrue()
    assertThat(intent.getBooleanExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, true)).isFalse()
  }
}
