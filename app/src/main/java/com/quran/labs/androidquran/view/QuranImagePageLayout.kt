package com.quran.labs.androidquran.view

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener
import com.quran.labs.androidquran.ui.util.PageController
import com.quran.labs.androidquran.util.QuranSettings
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Layout class for a single Arabic page of the Quran, with margins/background.
 * <p>
 * Note that the image of the Quran page to be displayed is set by users of this class by calling
 * {@link #getImageView()} and calling the appropriate methods on that view.
 */
open class QuranImagePageLayout(context: Context) : QuranPageLayout(context) {

  private lateinit var imageView: HighlightingImageView
  private var zoomScale = 1f
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
  private var downX = 0f
  private var downY = 0f
  private var handOffToPager = false
  private var startedAtPositiveXEdge = false
  private var startedAtNegativeXEdge = false
  private var startedAtPositiveYEdge = false
  private var startedAtNegativeYEdge = false

  init {
    this.initialize()
  }

  override fun generateContentView(context: Context, isLandscape: Boolean): View {
    imageView = HighlightingImageView(context).apply {
      adjustViewBounds = true
      setIsScrollable(isLandscape && shouldWrapWithScrollView(), isLandscape)
    }
    return imageView
  }

  override fun updateView(quranSettings: QuranSettings) {
    super.updateView(quranSettings)
    imageView.setNightMode(quranSettings.isNightMode, quranSettings.nightModeTextBrightness, quranSettings.nightModeBackgroundBrightness)
  }

  override fun setPageController(controller: PageController?, pageNumber: Int, skips: Int) {
    super.setPageController(controller, pageNumber, skips)
    val gestureDetector = GestureDetector(context, PageGestureDetector())
    val gestureListener = OnTouchListener { _, event ->
      if (zoomScale > MIN_ZOOM) {
        when (event.actionMasked) {
          MotionEvent.ACTION_DOWN -> {
            downX = event.x
            downY = event.y
            handOffToPager = false
            val maxX = maxTranslationX()
            val maxY = maxTranslationY()
            startedAtPositiveXEdge = imageView.translationX >= maxX - EDGE_TOLERANCE_PX
            startedAtNegativeXEdge = imageView.translationX <= -maxX + EDGE_TOLERANCE_PX
            startedAtPositiveYEdge = imageView.translationY >= maxY - EDGE_TOLERANCE_PX
            startedAtNegativeYEdge = imageView.translationY <= -maxY + EDGE_TOLERANCE_PX
            parent?.requestDisallowInterceptTouchEvent(true)
          }

          MotionEvent.ACTION_MOVE -> {
            val deltaX = event.x - downX
            val deltaY = event.y - downY
            if (!handOffToPager && hypot(deltaX, deltaY) > touchSlop) {
              handOffToPager = shouldHandOffToPager(
                deltaX,
                deltaY,
                startedAtPositiveXEdge,
                startedAtNegativeXEdge,
                startedAtPositiveYEdge,
                startedAtNegativeYEdge
              )
              if (handOffToPager) {
                val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                gestureDetector.onTouchEvent(cancel)
                cancel.recycle()
                parent?.requestDisallowInterceptTouchEvent(false)
                return@OnTouchListener false
              }
            }
          }

          MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            parent?.requestDisallowInterceptTouchEvent(false)
            if (handOffToPager) {
              handOffToPager = false
              return@OnTouchListener false
            }
          }
        }
      }
      val gestureHandled = gestureDetector.onTouchEvent(event)
      if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
        parent?.requestDisallowInterceptTouchEvent(false)
      }
      zoomScale > MIN_ZOOM || gestureHandled
    }
    imageView.setOnTouchListener(gestureListener)
    imageView.isClickable = true
    imageView.isLongClickable = true
  }

  fun getImageView(): HighlightingImageView = imageView

  fun zoomBy(delta: Float) {
    val newScale = (zoomScale + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
    if (newScale == zoomScale) {
      return
    }
    zoomScale = newScale
    if (zoomScale <= MIN_ZOOM) {
      resetZoom(animated = true)
    } else {
      imageView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
      imageView.pivotX = imageView.width / 2f
      imageView.pivotY = imageView.height / 2f
      imageView.animate()
        .scaleX(zoomScale)
        .scaleY(zoomScale)
        .setDuration(ZOOM_ANIMATION_DURATION_MS)
        .withEndAction {
          clampTranslation()
          imageView.setLayerType(View.LAYER_TYPE_NONE, null)
        }
        .start()
    }
  }

  private fun applyZoom() {
    imageView.pivotX = imageView.width / 2f
    imageView.pivotY = imageView.height / 2f
    imageView.scaleX = zoomScale
    imageView.scaleY = zoomScale
    clampTranslation()
    parent?.requestDisallowInterceptTouchEvent(false)
  }

  private fun resetZoom(animated: Boolean) {
    zoomScale = MIN_ZOOM
    if (animated) {
      imageView.animate()
        .scaleX(MIN_ZOOM)
        .scaleY(MIN_ZOOM)
        .translationX(0f)
        .translationY(0f)
        .setDuration(RESET_ANIMATION_DURATION_MS)
        .withEndAction {
          imageView.setLayerType(View.LAYER_TYPE_NONE, null)
          parent?.requestDisallowInterceptTouchEvent(false)
        }
        .start()
    } else {
      imageView.translationX = 0f
      imageView.translationY = 0f
      applyZoom()
      imageView.setLayerType(View.LAYER_TYPE_NONE, null)
    }
  }

  private fun clampTranslation() {
    val maxX = maxTranslationX()
    val maxY = maxTranslationY()
    imageView.translationX = imageView.translationX.coerceIn(-maxX, maxX)
    imageView.translationY = imageView.translationY.coerceIn(-maxY, maxY)
  }

  private fun maxTranslationX(): Float = imageView.width * (zoomScale - MIN_ZOOM) / 2f

  private fun maxTranslationY(): Float = imageView.height * (zoomScale - MIN_ZOOM) / 2f

  inner class PageGestureDetector : GestureDetector.SimpleOnGestureListener() {
    override fun onDown(e: MotionEvent): Boolean = true

    override fun onSingleTapUp(event: MotionEvent): Boolean {
      return pageController.handleTouchEvent(
        event,
        AyahSelectedListener.EventType.SINGLE_TAP,
        pageNumber
      )
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
      return pageController.handleTouchEvent(
        event,
        AyahSelectedListener.EventType.DOUBLE_TAP,
        pageNumber
      )
    }

    override fun onScroll(
      e1: MotionEvent?,
      e2: MotionEvent,
      distanceX: Float,
      distanceY: Float
    ): Boolean {
      if (zoomScale <= MIN_ZOOM) {
        return false
      }
      imageView.translationX -= distanceX
      imageView.translationY -= distanceY
      clampTranslation()
      parent?.requestDisallowInterceptTouchEvent(true)
      return true
    }

    override fun onLongPress(event: MotionEvent) {
      pageController.handleTouchEvent(
        event,
        AyahSelectedListener.EventType.LONG_PRESS,
        pageNumber
      )
    }
  }

  companion object {
    private const val MIN_ZOOM = 1f
    private const val MAX_ZOOM = 4f
    private const val RESET_ANIMATION_DURATION_MS = 160L
    private const val ZOOM_ANIMATION_DURATION_MS = 120L
    private const val EDGE_TOLERANCE_PX = 1f

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
