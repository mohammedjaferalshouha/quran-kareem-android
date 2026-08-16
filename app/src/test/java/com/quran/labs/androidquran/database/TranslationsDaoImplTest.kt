package com.quran.labs.androidquran.database

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.quran.data.model.VerseRange
import com.quran.labs.androidquran.fakes.FakeQuranFileUtils
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TranslationsDaoImplTest {

  @Test
  fun `missing arabic database keeps page accessibility optional`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dao = TranslationsDaoImpl(context, FakeQuranFileUtils.create(context))

    val result = dao.ayahs(VerseRange(1, 1, 1, 7, 1))

    assertThat(result).isEmpty()
  }
}
