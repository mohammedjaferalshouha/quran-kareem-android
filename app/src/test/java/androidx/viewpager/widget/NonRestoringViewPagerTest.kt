package androidx.viewpager.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NonRestoringViewPagerTest {

  @Test
  fun `vertical paging uses a short settled transition`() {
    assertThat(NonRestoringViewPager.VERTICAL_READING_TRANSITION_MS).isEqualTo(120)
  }

  @Test
  fun `upward gesture maps to forward rtl pager direction`() {
    val width = 400f
    val height = 800f

    val mappedAtBottom = NonRestoringViewPager.verticalToHorizontalX(700f, width, height)
    val mappedAtTop = NonRestoringViewPager.verticalToHorizontalX(100f, width, height)

    assertThat(mappedAtTop).isGreaterThan(mappedAtBottom)
  }

  @Test
  fun `horizontal coordinate maps proportionally to vertical coordinate`() {
    assertThat(NonRestoringViewPager.verticalToHorizontalY(100f, 400f, 800f))
      .isEqualTo(200f)
  }

  @Test
  fun `vertical gesture endpoints map across the full horizontal width`() {
    val width = 400f
    val height = 800f

    assertThat(NonRestoringViewPager.verticalToHorizontalX(0f, width, height))
      .isEqualTo(width)
    assertThat(NonRestoringViewPager.verticalToHorizontalX(height, width, height))
      .isEqualTo(0f)
  }

  @Test
  fun `horizontal gesture endpoints map across the full vertical height`() {
    val width = 400f
    val height = 800f

    assertThat(NonRestoringViewPager.verticalToHorizontalY(0f, width, height))
      .isEqualTo(0f)
    assertThat(NonRestoringViewPager.verticalToHorizontalY(width, width, height))
      .isEqualTo(height)
  }

  @Test
  fun `vertical transform matrix maps every sampled gesture point`() {
    val values = NonRestoringViewPager.verticalReadingTransformValues(400f, 800f)

    fun mappedX(x: Float, y: Float) = values[0] * x + values[1] * y + values[2]
    fun mappedY(x: Float, y: Float) = values[3] * x + values[4] * y + values[5]

    assertThat(mappedX(100f, 700f)).isEqualTo(50f)
    assertThat(mappedY(100f, 700f)).isEqualTo(200f)
    assertThat(mappedX(200f, 100f)).isEqualTo(350f)
    assertThat(mappedY(200f, 100f)).isEqualTo(400f)
  }

  @Test
  fun `dropped upward swipe recovers the next reading page`() {
    assertThat(
      NonRestoringViewPager.verticalGestureTarget(20, 700f, 100f, 100f, 604)
    ).isEqualTo(19)
  }

  @Test
  fun `dropped downward swipe recovers the previous reading page`() {
    assertThat(
      NonRestoringViewPager.verticalGestureTarget(20, 100f, 700f, 100f, 604)
    ).isEqualTo(21)
  }

  @Test
  fun `short gesture is never forced into a page transition`() {
    assertThat(
      NonRestoringViewPager.verticalGestureTarget(20, 500f, 450f, 100f, 604)
    ).isEqualTo(-1)
  }

  @Test
  fun `recovery never crosses the first or last page`() {
    assertThat(
      NonRestoringViewPager.verticalGestureTarget(0, 700f, 100f, 100f, 604)
    ).isEqualTo(-1)
    assertThat(
      NonRestoringViewPager.verticalGestureTarget(603, 100f, 700f, 100f, 604)
    ).isEqualTo(-1)
  }

  @Test
  fun `rapid upward gestures accumulate without losing pages`() {
    val currentItemAfterFirstGesture = 332
    var pendingDelta = 0

    repeat(19) {
      pendingDelta = NonRestoringViewPager.accumulateVerticalPageDelta(
        currentItemAfterFirstGesture, pendingDelta, -1, 604
      )
    }

    assertThat(currentItemAfterFirstGesture + pendingDelta).isEqualTo(313)
  }

  @Test
  fun `queued opposite gestures cancel deterministically`() {
    var pendingDelta = 0

    pendingDelta = NonRestoringViewPager.accumulateVerticalPageDelta(332, pendingDelta, -1, 604)
    pendingDelta = NonRestoringViewPager.accumulateVerticalPageDelta(332, pendingDelta, 1, 604)

    assertThat(pendingDelta).isEqualTo(0)
  }

  @Test
  fun `queued gestures stop at adapter boundaries`() {
    var firstPageDelta = 0
    var lastPageDelta = 0

    repeat(20) {
      firstPageDelta = NonRestoringViewPager.accumulateVerticalPageDelta(
        0, firstPageDelta, -1, 604
      )
      lastPageDelta = NonRestoringViewPager.accumulateVerticalPageDelta(
        603, lastPageDelta, 1, 604
      )
    }

    assertThat(firstPageDelta).isEqualTo(0)
    assertThat(lastPageDelta).isEqualTo(0)
  }

  @Test
  fun `stationary long press never queues a page`() {
    val direction = NonRestoringViewPager.verticalGestureDirection(400f, 400f, 100f)

    assertThat(direction).isEqualTo(0)
    assertThat(
      NonRestoringViewPager.accumulateVerticalPageDelta(332, 0, direction, 604)
    ).isEqualTo(0)
  }
}
