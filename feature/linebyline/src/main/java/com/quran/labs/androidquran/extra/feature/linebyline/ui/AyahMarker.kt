package com.quran.labs.androidquran.extra.feature.linebyline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer

@Composable
fun AyahMarker(
  lineId: Int,
  centerX: Float,
  centerY: Float,
  ringColor: Color,
  innerColor: Color,
  text: String,
  textColor: Color,
  textWidthRatio: Float
) {
  val textMeasurer = rememberTextMeasurer()

  Canvas(modifier = Modifier.fillMaxSize()) {
    val width = this.size.width
    val digitCount = text.count { it.isDigit() }.coerceAtLeast(1)
    val metrics = calculateAyahMarkerMetrics(width, digitCount, textWidthRatio)
    val strokeWidth = 0.003f * width
    val markerDimen = metrics.markerDiameter
    val radius = markerDimen / 2f

    val lastLineIndex = 14
    val lineHeight = this.size.width * 174 / 1080

    val xOffset = ((centerX * this.size.width) - (markerDimen / 2))
    val yStart = (this.size.height - lineHeight) / lastLineIndex * lineId
    val yOffset = (yStart + (centerY * lineHeight) - (markerDimen / 2))

    drawCircle(innerColor, radius, center = Offset(xOffset + radius, yOffset + radius))
    drawCircle(
      color = ringColor,
      radius = radius,
      center = Offset(xOffset + radius, yOffset + radius),
      style = Stroke(width = strokeWidth)
    )

    val fontSize = metrics.fontSize.toSp()
    val measurement =
      textMeasurer.measure(text, TextStyle.Default.copy(color = textColor, fontSize = fontSize))

    drawText(
      measurement,
      topLeft = Offset(
        xOffset + (radius - (measurement.size.width / 2)),
        yOffset + (radius - (measurement.size.height / 2)) + 4 - 1
      )
    )
  }
}

internal data class AyahMarkerMetrics(
  val markerDiameter: Int,
  val fontSize: Float
)

internal fun calculateAyahMarkerMetrics(
  width: Float,
  digitCount: Int,
  requestedTextWidthRatio: Float
): AyahMarkerMetrics {
  val textWidthRatio = when (digitCount.coerceIn(1, 3)) {
    1 -> ONE_DIGIT_TEXT_WIDTH_RATIO
    2 -> TWO_DIGIT_TEXT_WIDTH_RATIO
    else -> THREE_DIGIT_TEXT_WIDTH_RATIO
  }

  return AyahMarkerMetrics(
    markerDiameter = (width * MARKER_DIAMETER_RATIO).toInt().coerceAtLeast(1),
    fontSize = width * maxOf(requestedTextWidthRatio, textWidthRatio)
  )
}

private const val MARKER_DIAMETER_RATIO = 0.06f
private const val ONE_DIGIT_TEXT_WIDTH_RATIO = 0.044f
private const val TWO_DIGIT_TEXT_WIDTH_RATIO = 0.040f
private const val THREE_DIGIT_TEXT_WIDTH_RATIO = 0.034f
