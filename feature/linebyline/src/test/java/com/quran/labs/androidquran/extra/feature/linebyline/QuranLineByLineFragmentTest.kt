package com.quran.labs.androidquran.extra.feature.linebyline

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuranLineByLineFragmentTest {

  @Test
  fun `zoom scale applies deltas within safe bounds`() {
    assertThat(QuranLineByLineFragment.calculateZoomScale(1f, 0.35f)).isEqualTo(1.35f)
    assertThat(QuranLineByLineFragment.calculateZoomScale(1f, -0.35f)).isEqualTo(1f)
    assertThat(QuranLineByLineFragment.calculateZoomScale(3.9f, 0.35f)).isEqualTo(4f)
  }
}
