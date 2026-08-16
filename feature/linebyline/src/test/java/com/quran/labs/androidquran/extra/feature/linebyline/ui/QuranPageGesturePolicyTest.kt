package com.quran.labs.androidquran.extra.feature.linebyline.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuranPageGesturePolicyTest {
  private val touchSlop = 18f
  private val longPressTimeoutMillis = 500L

  @Test
  fun releasedPointerWithinTouchSlopDoesNotSelectAyah() {
    val decision = classifyQuranPageGesture(
      elapsedMillis = 120L,
      distanceFromDown = 8f,
      touchSlop = touchSlop,
      longPressTimeoutMillis = longPressTimeoutMillis,
      released = true
    )

    assertThat(decision).isEqualTo(QuranPageGestureDecision.RELEASE_WITHOUT_SELECTION)
  }

  @Test
  fun movementBeyondTouchSlopIsReservedForNavigation() {
    val decision = classifyQuranPageGesture(
      elapsedMillis = 120L,
      distanceFromDown = touchSlop + 0.1f,
      touchSlop = touchSlop,
      longPressTimeoutMillis = longPressTimeoutMillis,
      released = false
    )

    assertThat(decision).isEqualTo(QuranPageGestureDecision.PASS_TO_NAVIGATION)
  }

  @Test
  fun releaseAfterMovingBeyondTouchSlopIsNotTap() {
    val decision = classifyQuranPageGesture(
      elapsedMillis = 160L,
      distanceFromDown = touchSlop + 1f,
      touchSlop = touchSlop,
      longPressTimeoutMillis = longPressTimeoutMillis,
      released = true
    )

    assertThat(decision).isEqualTo(QuranPageGestureDecision.PASS_TO_NAVIGATION)
  }

  @Test
  fun heldPointerWithinTouchSlopStartsRangeSelection() {
    val decision = classifyQuranPageGesture(
      elapsedMillis = longPressTimeoutMillis,
      distanceFromDown = 4f,
      touchSlop = touchSlop,
      longPressTimeoutMillis = longPressTimeoutMillis,
      released = false
    )

    assertThat(decision).isEqualTo(QuranPageGestureDecision.START_RANGE_SELECTION)
  }

  @Test
  fun pointerWithinThresholdKeepsWaiting() {
    val decision = classifyQuranPageGesture(
      elapsedMillis = longPressTimeoutMillis - 1,
      distanceFromDown = touchSlop,
      touchSlop = touchSlop,
      longPressTimeoutMillis = longPressTimeoutMillis,
      released = false
    )

    assertThat(decision).isEqualTo(QuranPageGestureDecision.WAITING)
  }
}
