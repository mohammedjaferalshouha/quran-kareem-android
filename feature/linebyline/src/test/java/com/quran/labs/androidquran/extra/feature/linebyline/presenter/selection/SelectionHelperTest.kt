package com.quran.labs.androidquran.extra.feature.linebyline.presenter.selection

import com.google.common.truth.Truth.assertThat
import com.quran.data.model.SuraAyah
import com.quran.mobile.linebyline.data.dao.AyahHighlight
import org.junit.Test

class SelectionHelperTest {

  private val defaultWidth = 1080
  private val defaultHeight = 1920

  private fun buildHelper(width: Int = defaultWidth, height: Int = defaultHeight): SelectionHelper {
    return SelectionHelper().also { it.setPageDimensions(width, height) }
  }

  private fun buildHighlight(
    lineId: Int,
    left: Float = 0f,
    right: Float = 1f,
    sura: Int = 1,
    ayah: Int = 1
  ) = AyahHighlight(
    ayahId = 1,
    page = 1,
    sura = sura,
    ayah = ayah,
    lineId = lineId,
    left = left,
    right = right
  )

  // ---- selectionRectangle ----

  @Test
  fun `selectionRectangle returns null when page dimensions not set`() {
    // Arrange
    val helper = SelectionHelper()
    val highlight = buildHighlight(lineId = 0)

    // Act
    val result = helper.selectionRectangle(highlight)

    // Assert
    assertThat(result).isNull()
  }

  @Test
  fun `selectionRectangle returns non-null rectangle after page dimensions are set`() {
    // Arrange
    val helper = buildHelper()
    val highlight = buildHighlight(lineId = 0, left = 0f, right = 0.5f)

    // Act
    val result = helper.selectionRectangle(highlight)

    // Assert
    assertThat(result).isNotNull()
  }

  @Test
  fun `selectionRectangle rectangle has correct x coordinates scaled by width`() {
    // Arrange
    val width = 1080
    val helper = buildHelper(width = width)
    val highlight = buildHighlight(lineId = 0, left = 0.1f, right = 0.9f)

    // Act
    val result = helper.selectionRectangle(highlight)

    // Assert
    assertThat(result).isNotNull()
    val rect = requireNotNull(result)
    assertThat(rect.left).isWithin(1f).of(0.1f * width)
    assertThat(rect.right).isWithin(1f).of(0.9f * width)
  }

  // ---- modifySelectionRange ----

  @Test
  fun `modifySelectionRange returns null when page dimensions not set`() {
    // Arrange
    val helper = SelectionHelper()
    val highlights = listOf(buildHighlight(lineId = 0))

    // Act
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    // Assert
    assertThat(result).isNull()
  }

  @Test
  fun `modifySelectionRange returns null when no active selection point`() {
    // Arrange
    val helper = buildHelper()
    val highlights = listOf(buildHighlight(lineId = 0))

    // Act - no startSelection call
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    // Assert
    assertThat(result).isNull()
  }

  @Test
  fun `modifySelectionRange returns matching ayah when point lands on highlight`() {
    // Arrange
    val helper = buildHelper()
    val highlights = listOf(buildHighlight(lineId = 0, left = 0f, right = 1f, sura = 2, ayah = 5))

    // Act - start selection at top-left, then modify with zero offset so it stays at y=0
    helper.startSelection(x = 0f, y = 0f)
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    // Assert - y=0 should be on line 0
    assertThat(result).isNotNull()
    val ayah = requireNotNull(result)
    assertThat(ayah).isEqualTo(SuraAyah(2, 5))
  }

  @Test
  fun `modifySelectionRange returns null when point lands outside all highlights`() {
    // Arrange
    val helper = buildHelper()
    // highlight with right = 0.09f means x must be < 0.09 * 1080 ≈ 97
    // x=200 is well past this boundary so the filter excludes the highlight
    val highlights = listOf(buildHighlight(lineId = 0, left = 0f, right = 0.09f))

    // Act
    helper.startSelection(x = 200f, y = 0f)
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    // Assert
    assertThat(result).isNull()
  }

  @Test
  fun `modifySelectionRange uses both left and right boundaries for an exact match`() {
    val helper = buildHelper(width = 1000)
    val highlights = listOf(
      buildHighlight(lineId = 0, left = 0.6f, right = 0.9f, ayah = 9),
      buildHighlight(lineId = 0, left = 0.1f, right = 0.3f, ayah = 1)
    )

    helper.startSelection(x = 200f, y = 0f)
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    assertThat(result).isEqualTo(SuraAyah(1, 1))
  }

  @Test
  fun `modifySelectionRange expands the left edge by a limited amount`() {
    val helper = buildHelper(width = 1080)
    val highlights = listOf(buildHighlight(lineId = 0, left = 0.2f, right = 0.4f, ayah = 3))

    helper.startSelection(x = 205f, y = 0f)
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    assertThat(result).isEqualTo(SuraAyah(1, 3))
  }

  @Test
  fun `modifySelectionRange expands the right edge by a limited amount`() {
    val helper = buildHelper(width = 1080)
    val highlights = listOf(buildHighlight(lineId = 0, left = 0.2f, right = 0.4f, ayah = 4))

    helper.startSelection(x = 440f, y = 0f)
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    assertThat(result).isEqualTo(SuraAyah(1, 4))
  }

  @Test
  fun `modifySelectionRange chooses the nearest ayah between highlights within the maximum distance`() {
    val helper = buildHelper(width = 1000)
    val highlights = listOf(
      buildHighlight(lineId = 0, left = 0.1f, right = 0.2f, ayah = 5),
      buildHighlight(lineId = 0, left = 0.24f, right = 0.34f, ayah = 6)
    )

    helper.startSelection(x = 225f, y = 0f)
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    assertThat(result).isEqualTo(SuraAyah(1, 6))
  }

  @Test
  fun `modifySelectionRange returns null in a distant gap between highlights`() {
    val helper = buildHelper(width = 1000)
    val highlights = listOf(
      buildHighlight(lineId = 0, left = 0.1f, right = 0.2f, ayah = 7),
      buildHighlight(lineId = 0, left = 0.5f, right = 0.6f, ayah = 8)
    )

    helper.startSelection(x = 350f, y = 0f)
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    assertThat(result).isNull()
  }

  @Test
  fun `endSelection clears the selection point so modifySelectionRange returns null`() {
    // Arrange
    val helper = buildHelper()
    val highlights = listOf(buildHighlight(lineId = 0, left = 0f, right = 1f))

    // Act
    helper.startSelection(x = 0f, y = 0f)
    helper.endSelection()
    val result = helper.modifySelectionRange(0f, 0f, highlights)

    // Assert
    assertThat(result).isNull()
  }

  // ---- yForLine ----

  @Test
  fun `yForLine returns zero when page dimensions not set`() {
    // Arrange
    val helper = SelectionHelper()

    // Act
    val result = helper.yForLine(0)

    // Assert
    assertThat(result).isEqualTo(0)
  }

  @Test
  fun `yForLine returns non-negative value for line zero`() {
    // Arrange
    val helper = buildHelper()

    // Act
    val result = helper.yForLine(0)

    // Assert
    assertThat(result).isAtLeast(0)
  }

  @Test
  fun `yForLine returns greater value for later lines`() {
    // Arrange
    val helper = buildHelper()

    // Act
    val yLine0 = helper.yForLine(0)
    val yLine5 = helper.yForLine(5)

    // Assert
    assertThat(yLine5).isGreaterThan(yLine0)
  }

  // ---- setPageDimensions ----

  @Test
  fun `setPageDimensions can be called multiple times without error`() {
    // Arrange
    val helper = SelectionHelper()

    // Act
    helper.setPageDimensions(1080, 1920)
    helper.setPageDimensions(1080, 1920)
    helper.setPageDimensions(720, 1280)

    // Assert - no exception thrown, and selection rectangle works
    val result = helper.selectionRectangle(buildHighlight(lineId = 0))
    assertThat(result).isNotNull()
  }
}
