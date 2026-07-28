package com.quran.labs.androidquran.view

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener
import com.quran.labs.androidquran.ui.util.PageController
import com.quran.labs.androidquran.util.QuranSettings

/**
 * Layout class for a single Arabic page of the Quran, with margins/background.
 * <p>
 * Note that the image of the Quran page to be displayed is set by users of this class by calling
 * {@link #getImageView()} and calling the appropriate methods on that view.
 */
open class QuranImagePageLayout(context: Context) : QuranPageLayout(context) {

  private lateinit var imageView: HighlightingImageView
  private var zoomScale = 1f

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
      if (event.actionMasked == MotionEvent.ACTION_DOWN && zoomScale > MIN_ZOOM) {
        parent?.requestDisallowInterceptTouchEvent(true)
      }
      val gestureHandled = gestureDetector.onTouchEvent(event)
      if ((event.actionMasked == MotionEvent.ACTION_UP ||
          event.actionMasked == MotionEvent.ACTION_CANCEL) && zoomScale <= MIN_ZOOM) {
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
      parent?.requestDisallowInterceptTouchEvent(true)
    }
  }

  private fun applyZoom() {
    imageView.pivotX = imageView.width / 2f
    imageView.pivotY = imageView.height / 2f
    imageView.scaleX = zoomScale
    imageView.scaleY = zoomScale
    clampTranslation()
    parent?.requestDisallowInterceptTouchEvent(zoomScale > MIN_ZOOM)
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
    val maxX = imageView.width * (zoomScale - 1f) / 2f
    val maxY = imageView.height * (zoomScale - 1f) / 2f
    imageView.translationX = imageView.translationX.coerceIn(-maxX, maxX)
    imageView.translationY = imageView.translationY.coerceIn(-maxY, maxY)
  }

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
  }
}
