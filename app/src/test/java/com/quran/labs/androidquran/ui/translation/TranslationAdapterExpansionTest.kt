package com.quran.labs.androidquran.ui.translation

import com.google.common.truth.Truth.assertThat
import com.quran.labs.androidquran.common.QuranAyahInfo
import org.junit.Test

class TranslationAdapterExpansionTest {
  @Test
  fun keepsExpandedTafseerWhenTheSamePageIsRefreshed() {
    val expansions = mutableSetOf(42 to 7L, 99 to 7L)
    val rows = listOf(
      TranslationViewRow(
        TranslationViewRow.Type.TRANSLATION_TEXT,
        QuranAyahInfo(2, 35, null, emptyList(), 42),
        "تفسير طويل",
        0,
        7L
      )
    )

    retainVisibleTafseerExpansions(expansions, rows)

    assertThat(expansions).containsExactly(42 to 7L)
  }

  @Test
  fun removesExpansionWhenItsTafseerSourceIsNoLongerVisible() {
    val expansions = mutableSetOf(42 to 8L)
    val rows = listOf(
      TranslationViewRow(
        TranslationViewRow.Type.TRANSLATION_TEXT,
        QuranAyahInfo(2, 35, null, emptyList(), 42),
        "تفسير طويل",
        0,
        7L
      )
    )

    retainVisibleTafseerExpansions(expansions, rows)

    assertThat(expansions).isEmpty()
  }

  @Test
  fun expansionKeysRoundTripThroughSavedState() {
    val keys = listOf(42 to 7L, 6236 to 9912345678L)

    val restored = decodeTafseerExpansionState(encodeTafseerExpansionState(keys))

    assertThat(restored).containsExactlyElementsIn(keys).inOrder()
  }
}
