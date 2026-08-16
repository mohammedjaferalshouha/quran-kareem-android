package com.quran.labs.feature.autoquran.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuranBrowsableAudioPlaybackServiceTest {
  @Test
  fun `application controller is accepted`() {
    assertThat(
      isTrustedMediaController(
        applicationUid = 1000,
        controllerUid = 1000,
        controllerIsTrusted = false
      )
    ).isTrue()
  }

  @Test
  fun `trusted external controller is accepted`() {
    assertThat(
      isTrustedMediaController(
        applicationUid = 1000,
        controllerUid = 2000,
        controllerIsTrusted = true
      )
    ).isTrue()
  }

  @Test
  fun `untrusted external controller is rejected`() {
    assertThat(
      isTrustedMediaController(
        applicationUid = 1000,
        controllerUid = 2000,
        controllerIsTrusted = false
      )
    ).isFalse()
  }
}
