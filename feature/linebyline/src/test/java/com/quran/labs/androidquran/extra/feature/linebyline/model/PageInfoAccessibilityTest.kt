package com.quran.labs.androidquran.extra.feature.linebyline.model

import com.google.common.truth.Truth.assertThat
import com.quran.data.model.QuranText
import org.junit.Test

class PageInfoAccessibilityTest {

  @Test
  fun `page accessibility text preserves ayah order and numbers`() {
    val text = buildAyahAccessibilityText(QuranText(1, 2, "الآية الثانية"))

    assertThat(text).isEqualTo("الآية الثانية ۝2")
  }
}
