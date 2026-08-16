package com.quran.labs.androidquran.extra.feature.linebyline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import com.quran.data.source.PageContentType
import com.quran.labs.androidquran.extra.feature.linebyline.model.HighlightType
import com.quran.labs.androidquran.extra.feature.linebyline.model.LineModel
import com.quran.labs.androidquran.extra.feature.linebyline.model.PageInfo
import com.quran.labs.androidquran.extra.feature.linebyline.ui.modifier.pageGradient
import com.quran.mobile.linebyline.data.dao.AyahHighlight
import com.quran.mobile.linebyline.data.dao.AyahMarkerInfo
import com.quran.mobile.linebyline.data.dao.SuraHeader
import com.quran.mobile.linebyline.ui.renderer.composable.QuranLineLayout
import com.quran.mobile.linebyline.ui.renderer.composable.QuranPageLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun QuranPage(
  pageInfo: PageInfo,
  pageContentType: PageContentType,
  isScrollable: Boolean,
  headerComposable: @Composable () -> Unit,
  footerComposable: @Composable () -> Unit,
  quranLineComposable: @Composable (LineModel) -> Unit,
  suraHeaderComposable: @Composable (SuraHeader) -> Unit,
  ayahMarkerInfoComposable: @Composable (AyahMarkerInfo) -> Unit,
  highlightComposable: @Composable (AyahHighlight, Color) -> Unit,
  sidelinesComposable: @Composable () -> Unit,
  onPagePositioned: (Float, Float, Int, Int) -> Unit,
  onSelectionStart: (Float, Float) -> Unit,
  onSelectionModified: (Float, Float) -> Unit,
  onSelectionEnd: () -> Unit,
  modifier: Modifier = Modifier,
  isNightMode: Boolean = false,
  nightModeBackgroundBrightness: Int = 0
) {
  val mod = if (isNightMode) {
    modifier.background(
      Color(
        nightModeBackgroundBrightness,
        nightModeBackgroundBrightness,
        nightModeBackgroundBrightness
      ))
  } else {
    val skip = pageInfo.skippedPageCount
    val target = skip % 2
    modifier.pageGradient(pageInfo.page % 2 == target)
  }

  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()
  val hostView = LocalView.current

  LaunchedEffect(key1 = pageInfo.targetScrollPosition) {
    if (pageInfo.targetScrollPosition >= 0 && pageInfo.targetScrollPosition != scrollState.value) {
      coroutineScope.launch {
        scrollState.animateScrollTo(pageInfo.targetScrollPosition)
      }
    }
  }

  val ratio = (pageContentType as PageContentType.Line).ratio
  val allowLinesToOverlap = pageContentType.allowOverlapOfLines
  Box(modifier = mod.fillMaxSize()) {
    QuranPageLayout(
      pageInfo.page,
      isScrollable = isScrollable,
      scrollState = scrollState,
      showSidelines = pageInfo.showSidelines,
      header = headerComposable,
      footer = footerComposable,
      sidelines = sidelinesComposable
    ) {
      Box {
        QuranLineLayout(
          lineHeightWidthRatio = ratio,
          allowLinesToOverlap = allowLinesToOverlap,
          modifier = Modifier
            .pointerInput(hostView) {
              detectAyahSelectionGestures(
                onDragStart = { onSelectionStart(it.x, it.y) },
                onDragEnd = { onSelectionEnd() },
                onRangeSelectionOwnershipChanged = { ownsGesture ->
                  hostView.parent?.requestDisallowInterceptTouchEvent(ownsGesture)
                }
              ) { change, dragAmount ->
                onSelectionModified(dragAmount.x, dragAmount.y)
              }
            }
            .onGloballyPositioned {
              val position = it.positionInWindow()
              onPagePositioned(position.x, position.y, it.size.width, it.size.height)
            }
        ) {
          pageInfo.lineModels.forEach { lineInfo ->
            Box {
              val lineId = lineInfo.lineId
              quranLineComposable(lineInfo)

              // Sura Headers
              val suraHeaders = pageInfo.suraHeaders.filter { it.lineId == lineId }
              suraHeaders.forEach {
                suraHeaderComposable(it)
              }
            }
          }
        }

        // highlights and ayah markers all overlay the QuranLineLayout
        pageInfo.lineModels.forEach { lineInfo ->
          val lineId = lineInfo.lineId

          // Ayah Markers
          val markers = pageInfo.ayahMarkers.filter { it.lineId == lineId }
          markers.forEach {
            ayahMarkerInfoComposable(it)
          }

          // Highlights
          val highlights = pageInfo.ayahHighlights.filter { highlightAyah ->
            highlightAyah.ayahHighlights.any { it.lineId == lineId }
          }
          highlights.forEach { highlightAyah ->
            val color = when (highlightAyah.highlightType) {
              HighlightType.SELECTION -> Color(0x46, 0x94, 0xa6, 0x40)
              HighlightType.AUDIO -> Color(0x46, 0xa6, 0x46, 0x40)
              HighlightType.AUDIO_WORD -> Color(0xff, 0xb3, 0x3d, 0x60)
              HighlightType.BOOKMARK -> Color(0xa4, 0xa4, 0xa4, 0x40)
            }

            highlightAyah.ayahHighlights
              .filter { it.lineId == lineId }
              .forEach {
                highlightComposable(it, color)
              }
          }
        }
      }
    }
  }
}

private suspend fun PointerInputScope.detectAyahSelectionGestures(
  onDragStart: (Offset) -> Unit,
  onDragEnd: () -> Unit,
  onRangeSelectionOwnershipChanged: (Boolean) -> Unit,
  onDrag: (PointerInputChange, Offset) -> Unit
) {
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    var latestChange = down
    var decision = QuranPageGestureDecision.WAITING

    val completedBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
      while (decision == QuranPageGestureDecision.WAITING) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || change.isConsumed) {
          decision = QuranPageGestureDecision.PASS_TO_NAVIGATION
          break
        }

        latestChange = change
        decision = classifyQuranPageGesture(
          elapsedMillis = change.uptimeMillis - down.uptimeMillis,
          distanceFromDown = (change.position - down.position).getDistance(),
          touchSlop = viewConfiguration.touchSlop,
          longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis,
          released = change.changedToUpIgnoreConsumed()
        )

        if (decision == QuranPageGestureDecision.WAITING) {
          val finalEvent = awaitPointerEvent(PointerEventPass.Final)
          if (finalEvent.changes.any { it.id == down.id && it.isConsumed }) {
            decision = QuranPageGestureDecision.PASS_TO_NAVIGATION
          }
        }
      }
      true
    }

    if (completedBeforeTimeout == null && decision == QuranPageGestureDecision.WAITING) {
      decision = QuranPageGestureDecision.START_RANGE_SELECTION
    }

    when (decision) {
      QuranPageGestureDecision.RELEASE_WITHOUT_SELECTION -> Unit
      QuranPageGestureDecision.START_RANGE_SELECTION -> {
        onRangeSelectionOwnershipChanged(true)
        try {
          onDragStart(latestChange.position)
          drag(latestChange.id) { change ->
            val dragAmount = change.positionChange()
            if (dragAmount != Offset.Zero) {
              onDrag(change, dragAmount)
            }
            change.consume()
          }
          currentEvent.changes.forEach { change ->
            if (change.changedToUp()) {
              change.consume()
            }
          }
          onDragEnd()
        } finally {
          onRangeSelectionOwnershipChanged(false)
        }
      }
      QuranPageGestureDecision.PASS_TO_NAVIGATION,
      QuranPageGestureDecision.WAITING -> Unit
    }
  }
}

internal enum class QuranPageGestureDecision {
  WAITING,
  RELEASE_WITHOUT_SELECTION,
  START_RANGE_SELECTION,
  PASS_TO_NAVIGATION
}

internal fun classifyQuranPageGesture(
  elapsedMillis: Long,
  distanceFromDown: Float,
  touchSlop: Float,
  longPressTimeoutMillis: Long,
  released: Boolean
): QuranPageGestureDecision {
  return when {
    distanceFromDown > touchSlop -> QuranPageGestureDecision.PASS_TO_NAVIGATION
    released -> QuranPageGestureDecision.RELEASE_WITHOUT_SELECTION
    elapsedMillis >= longPressTimeoutMillis -> QuranPageGestureDecision.START_RANGE_SELECTION
    else -> QuranPageGestureDecision.WAITING
  }
}
