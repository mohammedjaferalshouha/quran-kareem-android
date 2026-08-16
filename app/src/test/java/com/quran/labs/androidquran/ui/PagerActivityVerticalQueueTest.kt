package com.quran.labs.androidquran.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PagerActivityVerticalQueueTest {

  @Test
  fun `rapid forward requests are retained`() {
    var pending = 0
    repeat(10) {
      pending = PagerActivity.enqueueVerticalPageDelta(pending, 1)
    }

    assertThat(pending).isEqualTo(10)
  }

  @Test
  fun `opposite requests cancel instead of creating stale transitions`() {
    var pending = PagerActivity.enqueueVerticalPageDelta(0, 1)
    pending = PagerActivity.enqueueVerticalPageDelta(pending, -1)

    assertThat(pending).isEqualTo(0)
  }

  @Test
  fun `pending requests have a safe bound`() {
    var pending = 0
    repeat(100) {
      pending = PagerActivity.enqueueVerticalPageDelta(pending, 1)
    }

    assertThat(pending).isEqualTo(12)
  }
}
