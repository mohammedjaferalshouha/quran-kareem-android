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

  @Test
  fun `zoom by uses live layout scale after gesture zoom`() {
    val currentScale = QuranLineByLineFragment.resolveCurrentZoomScale(
      savedScale = 1f,
      layoutScale = 2f
    )
    assertThat(currentScale).isEqualTo(2f)
    assertThat(QuranLineByLineFragment.calculateZoomScale(currentScale, -0.35f)).isEqualTo(1.65f)
    assertThat(
      QuranLineByLineFragment.resolveCurrentZoomScale(savedScale = 1.5f, layoutScale = null)
    ).isEqualTo(1.5f)
  }
}
