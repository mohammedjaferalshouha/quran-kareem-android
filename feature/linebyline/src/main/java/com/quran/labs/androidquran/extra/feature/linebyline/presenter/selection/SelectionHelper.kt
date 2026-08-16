package com.quran.labs.androidquran.extra.feature.linebyline.presenter.selection

import com.quran.data.model.SuraAyah
import com.quran.data.model.selection.SelectionRectangle
import com.quran.mobile.linebyline.data.dao.AyahHighlight
import dev.zacsweers.metro.Inject

class SelectionHelper @Inject constructor() {
  private var currentSelectionPoint: Point? = null
  private var lineCalculation: LineCalculation? = null

  fun setPageDimensions(width: Int, height: Int) {
    lineCalculation = lineCalculation.from(width, height)
  }

  fun startSelection(x: Float, y: Float) {
    currentSelectionPoint = Point(x, y)
  }

  fun selectionRectangle(ayahHighlight: AyahHighlight): SelectionRectangle? {
    val lineCalculation = lineCalculation ?: return null
    val (minY, maxY) = lineCalculation.lineRangeFor(ayahHighlight.lineId)
    val minX = ayahHighlight.left * lineCalculation.width
    val maxX = ayahHighlight.right * lineCalculation.width
    return SelectionRectangle(minX, minY.toFloat(), maxX, maxY.toFloat())
  }

  fun modifySelectionRange(offsetX: Float, offsetY: Float, highlights: List<AyahHighlight>): SuraAyah? {
    val lineCalculation = lineCalculation ?: return null
    val previousPoint = currentSelectionPoint ?: return null
    val currentPoint = previousPoint.withOffset(offsetX, offsetY)

    val lineId = lineCalculation.lineIndexForY(currentPoint.y)
    val result = if (lineId > -1) {
      val lineHighlights = highlights.filter { it.lineId == lineId }
      val width = lineCalculation.width.toFloat()
      val horizontalHitSlop = width * HORIZONTAL_HIT_SLOP_RATIO
      val maximumNearestDistance = width * MAXIMUM_NEAREST_DISTANCE_RATIO

      val exactMatch = lineHighlights
        .firstOrNull { currentPoint.x in (it.left * width)..(it.right * width) }

      val paddedMatch = exactMatch ?: lineHighlights
        .filter { horizontalDistance(currentPoint.x, it, width) <= horizontalHitSlop }
        .minByOrNull { horizontalDistance(currentPoint.x, it, width) }

      val nearestMatch = paddedMatch ?: lineHighlights
        .minByOrNull { horizontalDistance(currentPoint.x, it, width) }
        ?.takeIf {
          horizontalDistance(currentPoint.x, it, width) <= maximumNearestDistance
        }

      nearestMatch?.let { SuraAyah(it.sura, it.ayah) }
    } else {
      null
    }
    currentSelectionPoint = currentPoint
    return result
  }

  fun endSelection() {
    currentSelectionPoint = null
  }

  fun yForLine(lineId: Int): Int {
    return lineCalculation?.lineRangeFor(lineId)?.first ?: 0
  }

  private class Point(val x: Float, val y: Float) {
    fun withOffset(offsetX: Float, offsetY: Float) =
      Point(x + offsetX, y + offsetY)
  }

  private fun horizontalDistance(x: Float, highlight: AyahHighlight, width: Float): Float {
    val left = highlight.left * width
    val right = highlight.right * width
    return when {
      x < left -> left - x
      x > right -> x - right
      else -> 0f
    }
  }

  private companion object {
    const val HORIZONTAL_HIT_SLOP_RATIO = 12f / 1080f
    const val MAXIMUM_NEAREST_DISTANCE_RATIO = 24f / 1080f
  }
}
