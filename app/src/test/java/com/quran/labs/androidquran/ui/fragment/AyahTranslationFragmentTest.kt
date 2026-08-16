package com.quran.labs.androidquran.ui.fragment

import com.google.common.truth.Truth.assertThat
import com.quran.mobile.translation.model.LocalTranslation
import org.junit.Test

class AyahTranslationFragmentTest {

  @Test
  fun `keeps preferred tafsir when it remains installed`() {
    val translations = listOf(translation("first.db"), translation("preferred.db"))

    val result = selectAyahTafsir(translations, setOf("first.db"), "preferred.db")

    assertThat(result?.filename).isEqualTo("preferred.db")
  }

  @Test
  fun `chooses one active tafsir when no preference exists`() {
    val translations = listOf(translation("first.db"), translation("active.db"))

    val result = selectAyahTafsir(translations, setOf("active.db"), null)

    assertThat(result?.filename).isEqualTo("active.db")
  }

  @Test
  fun `falls back to first installed source when active set is empty`() {
    val translations = listOf(translation("first.db"), translation("second.db"))

    val result = selectAyahTafsir(translations, emptySet(), null)

    assertThat(result?.filename).isEqualTo("first.db")
  }

  @Test
  fun `returns null when no source is installed`() {
    assertThat(selectAyahTafsir(emptyList(), setOf("missing.db"), "missing.db")).isNull()
  }

  private fun translation(filename: String) = LocalTranslation(filename = filename)
}
