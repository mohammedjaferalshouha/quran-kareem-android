package com.quran.labs.androidquran.extra.feature.linebyline

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Keeps a zoomed line-by-line page inside a fixed viewport while allowing the
 * reader to pan to every edge. Taps are still dispatched to the Quran page;
 * only a real drag is intercepted for panning.
 */
class LineByLineZoomLayout(context: Context) : FrameLayout(context) {
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
  private var contentView: View? = null
  private var zoomScale = MIN_ZOOM
  private var downX = 0f
  private var downY = 0f
  private var lastX = 0f
  private var lastY = 0f
  private var isPanning = false
  private var isScaling = false
  private var consumeUntilUp = false
  private var childCancelled = false
  private var handOffToPager = false
  private var startedAtPositiveXEdge = false
  private var startedAtNegativeXEdge = false
  private var startedAtPositiveYEdge = false
  private var startedAtNegativeYEdge = false

  private val scaleDetector = ScaleGestureDetector(
    context,
    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
      override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        isScaling = true
        isPanning = false
        handOffToPager = false
        consumeUntilUp = true
        parent?.requestDisallowInterceptTouchEvent(true)
        return true
      }

      override fun onScale(detector: ScaleGestureDetector): Boolean {
        val nextScale = (zoomScale * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (abs(nextScale - zoomScale) >= 0.005f) {
          setZoomScale(nextScale, animated = false)
        }
        return true
      }

      override fun onScaleEnd(detector: ScaleGestureDetector) {
        isScaling = false
      }
    }
  )

  private val gestureDetector = GestureDetector(
    context,
    object : GestureDetector.SimpleOnGestureListener() {
      override fun onDoubleTap(e: MotionEvent): Boolean {
        val targetScale = if (zoomScale > MIN_ZOOM + 0.05f) MIN_ZOOM else DOUBLE_TAP_ZOOM
        setZoomScale(targetScale, animated = true)
        consumeUntilUp = true
        return true
      }
    }
  )

  fun setContent(content: View) {
    removeAllViews()
    contentView = content
    addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    applyZoom(animated = false)
  }

  fun zoomBy(delta: Float) {
    setZoomScale(calculateZoomScale(zoomScale, delta), animated = true)
  }

  fun setZoomScale(scale: Float, animated: Boolean = false) {
    val newScale = scale.coerceIn(MIN_ZOOM, MAX_ZOOM)
    if (newScale == zoomScale && !animated) {
      applyZoom(animated = false)
      return
    }
    zoomScale = newScale
    applyZoom(animated)
  }

  fun currentZoomScale(): Float = zoomScale

  override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
    super.onSizeChanged(width, height, oldWidth, oldHeight)
    applyZoom(animated = false)
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    val content = contentView ?: return super.dispatchTouchEvent(event)
    val action = event.actionMasked
    if (action == MotionEvent.ACTION_DOWN) {
      childCancelled = false
      consumeUntilUp = false
      isScaling = false
    }

    scaleDetector.onTouchEvent(event)
    gestureDetector.onTouchEvent(event)

    if (event.pointerCount > 1 || isScaling || consumeUntilUp) {
      parent?.requestDisallowInterceptTouchEvent(true)
      if (!childCancelled) {
        childCancelled = true
        cancelChildTouch(event)
      }
      if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        isScaling = false
        isPanning = false
        consumeUntilUp = false
        childCancelled = false
        parent?.requestDisallowInterceptTouchEvent(false)
      }
      return true
    }

    if (zoomScale <= MIN_ZOOM) {
      if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        parent?.requestDisallowInterceptTouchEvent(false)
      }
      return super.dispatchTouchEvent(event)
    }

    when (action) {
      MotionEvent.ACTION_DOWN -> {
        downX = event.x
        downY = event.y
        lastX = event.x
        lastY = event.y
        isPanning = false
        handOffToPager = false
        val maxX = maxTranslationX()
        val maxY = maxTranslationY()
        startedAtPositiveXEdge = content.translationX >= maxX - EDGE_TOLERANCE_PX
        startedAtNegativeXEdge = content.translationX <= -maxX + EDGE_TOLERANCE_PX
        startedAtPositiveYEdge = content.translationY >= maxY - EDGE_TOLERANCE_PX
        startedAtNegativeYEdge = content.translationY <= -maxY + EDGE_TOLERANCE_PX
        parent?.requestDisallowInterceptTouchEvent(true)
      }

      MotionEvent.ACTION_MOVE -> {
        val totalDeltaX = event.x - downX
        val totalDeltaY = event.y - downY
        if (!isPanning && !handOffToPager && hypot(totalDeltaX, totalDeltaY) > touchSlop) {
          handOffToPager = shouldHandOffToPager(
            deltaX = totalDeltaX,
            deltaY = totalDeltaY,
            startedAtPositiveXEdge = startedAtPositiveXEdge,
            startedAtNegativeXEdge = startedAtNegativeXEdge,
            startedAtPositiveYEdge = startedAtPositiveYEdge,
            startedAtNegativeYEdge = startedAtNegativeYEdge
          )
          if (handOffToPager) {
            cancelChildTouch(event)
            parent?.requestDisallowInterceptTouchEvent(false)
            return false
          }
          isPanning = true
          cancelChildTouch(event)
        }
        if (isPanning) {
          content.translationX += event.x - lastX
          content.translationY += event.y - lastY
          clampTranslation(content)
          lastX = event.x
          lastY = event.y
          return true
        }
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        parent?.requestDisallowInterceptTouchEvent(false)
        if (handOffToPager) {
          handOffToPager = false
          return false
        }
        if (isPanning) {
          isPanning = false
          return true
        }
      }
    }
    lastX = event.x
    lastY = event.y
    return super.dispatchTouchEvent(event)
  }

  private fun cancelChildTouch(event: MotionEvent) {
    val cancel = MotionEvent.obtain(event)
    cancel.setAction(MotionEvent.ACTION_CANCEL)
    super.dispatchTouchEvent(cancel)
    cancel.recycle()
  }

  private fun applyZoom(animated: Boolean) {
    val content = contentView ?: return
    content.pivotX = content.width / 2f
    content.pivotY = content.height / 2f
    content.animate().cancel()
    if (zoomScale <= MIN_ZOOM) {
      if (animated) {
        content.animate()
          .scaleX(MIN_ZOOM)
          .scaleY(MIN_ZOOM)
          .translationX(0f)
          .translationY(0f)
          .setDuration(RESET_ANIMATION_DURATION_MS)
          .withEndAction { parent?.requestDisallowInterceptTouchEvent(false) }
          .start()
      } else {
        content.scaleX = MIN_ZOOM
        content.scaleY = MIN_ZOOM
        content.translationX = 0f
        content.translationY = 0f
        parent?.requestDisallowInterceptTouchEvent(false)
      }
    } else if (animated) {
      content.setLayerType(View.LAYER_TYPE_HARDWARE, null)
      content.animate()
        .scaleX(zoomScale)
        .scaleY(zoomScale)
        .setDuration(ZOOM_ANIMATION_DURATION_MS)
        .withEndAction {
          clampTranslation(content)
          content.setLayerType(View.LAYER_TYPE_NONE, null)
        }
        .start()
    } else {
      content.scaleX = zoomScale
      content.scaleY = zoomScale
      clampTranslation(content)
    }
  }

  private fun clampTranslation(content: View) {
    val maxX = maxTranslationX()
    val maxY = maxTranslationY()
    content.translationX = content.translationX.coerceIn(-maxX, maxX)
    content.translationY = content.translationY.coerceIn(-maxY, maxY)
  }

  private fun maxTranslationX(): Float = width * (zoomScale - MIN_ZOOM) / 2f

  private fun maxTranslationY(): Float = height * (zoomScale - MIN_ZOOM) / 2f

  companion object {
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 4f
    private const val DOUBLE_TAP_ZOOM = 2f
    private const val RESET_ANIMATION_DURATION_MS = 160L
    private const val ZOOM_ANIMATION_DURATION_MS = 120L
    private const val EDGE_TOLERANCE_PX = 1f

    internal fun calculateZoomScale(currentScale: Float, delta: Float): Float {
      return (currentScale + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    internal fun shouldHandOffToPager(
      deltaX: Float,
      deltaY: Float,
      startedAtPositiveXEdge: Boolean,
      startedAtNegativeXEdge: Boolean,
      startedAtPositiveYEdge: Boolean,
      startedAtNegativeYEdge: Boolean
    ): Boolean {
      return if (abs(deltaX) > abs(deltaY)) {
        (deltaX > 0f && startedAtPositiveXEdge) ||
          (deltaX < 0f && startedAtNegativeXEdge)
      } else {
        (deltaY > 0f && startedAtPositiveYEdge) ||
          (deltaY < 0f && startedAtNegativeYEdge)
      }
    }
  }
}
