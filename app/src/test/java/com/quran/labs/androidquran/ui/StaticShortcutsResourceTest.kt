package com.quran.labs.androidquran.ui

import com.google.common.truth.Truth.assertThat
import com.quran.labs.androidquran.BuildConfig
import org.junit.Test
import java.io.File

class StaticShortcutsResourceTest {

  @Test
  fun `madani shortcuts target release application id`() {
    assertTargetPackages(
      file = File("src/madani/res/xml/shortcuts.xml"),
      expectedPackage = BuildConfig.APPLICATION_ID.removeSuffix(DEBUG_SUFFIX)
    )
  }

  @Test
  fun `debug shortcuts target debug application id`() {
    assertThat(BuildConfig.APPLICATION_ID).endsWith(DEBUG_SUFFIX)
    assertTargetPackages(
      file = File("src/debug/res/xml/shortcuts.xml"),
      expectedPackage = BuildConfig.APPLICATION_ID
    )
  }

  private fun assertTargetPackages(file: File, expectedPackage: String) {
    assertThat(file.isFile).isTrue()
    val targetPackages = TARGET_PACKAGE_REGEX.findAll(file.readText())
      .map { it.groupValues[1] }
      .toList()

    assertThat(targetPackages).containsExactly(expectedPackage, expectedPackage)
  }

  private companion object {
    const val DEBUG_SUFFIX = ".debug"
    val TARGET_PACKAGE_REGEX = Regex("""android:targetPackage="([^"]+)"""")
  }
}
