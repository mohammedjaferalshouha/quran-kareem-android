package com.quran.labs.androidquran.extra.feature.linebyline.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AyahMarkerTest {

  @Test
  fun `marker diameter is larger than the previous marker`() {
    val metrics = calculateAyahMarkerMetrics(
      width = 1080f,
      digitCount = 1,
      requestedTextWidthRatio = 0.037f
    )

    assertThat(metrics.markerDiameter).isEqualTo(64)
    assertThat(metrics.markerDiameter).isGreaterThan(54)
  }

  @Test
  fun `one digit uses the largest text size`() {
    val metrics = calculateAyahMarkerMetrics(
      width = 1080f,
      digitCount = 1,
      requestedTextWidthRatio = 0.037f
    )

    assertThat(metrics.fontSize).isWithin(0.01f).of(47.52f)
  }

  @Test
  fun `two digits use a clear text size within the marker`() {
    val metrics = calculateAyahMarkerMetrics(
      width = 1080f,
      digitCount = 2,
      requestedTextWidthRatio = 0.037f
    )

    assertThat(metrics.fontSize).isWithin(0.01f).of(43.2f)
    assertThat(metrics.fontSize).isLessThan(metrics.markerDiameter.toFloat())
  }

  @Test
  fun `three digits use a smaller text size within the marker`() {
    val metrics = calculateAyahMarkerMetrics(
      width = 1080f,
      digitCount = 3,
      requestedTextWidthRatio = 0.03f
    )

    assertThat(metrics.fontSize).isWithin(0.01f).of(36.72f)
    assertThat(metrics.fontSize).isLessThan(metrics.markerDiameter.toFloat())
  }

  @Test
  fun `marker and text scale proportionally with page width`() {
    val small = calculateAyahMarkerMetrics(
      width = 540f,
      digitCount = 2,
      requestedTextWidthRatio = 0.037f
    )
    val large = calculateAyahMarkerMetrics(
      width = 1080f,
      digitCount = 2,
      requestedTextWidthRatio = 0.037f
    )

    assertThat(large.markerDiameter).isEqualTo(small.markerDiameter * 2)
    assertThat(large.fontSize).isWithin(0.01f).of(small.fontSize * 2)
  }

  @Test
  fun `four or more digits are constrained to the three digit size`() {
    val threeDigits = calculateAyahMarkerMetrics(
      width = 1080f,
      digitCount = 3,
      requestedTextWidthRatio = 0.03f
    )
    val moreDigits = calculateAyahMarkerMetrics(
      width = 1080f,
      digitCount = 4,
      requestedTextWidthRatio = 0.03f
    )

    assertThat(moreDigits).isEqualTo(threeDigits)
  }
}
