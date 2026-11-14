package com.quran.labs.androidquran.extra.feature.linebyline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class QuranLineByLineFragment : Fragment() {
  private var zoomScale = LineByLineZoomLayout.MIN_ZOOM
  private var zoomLayout: LineByLineZoomLayout? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    zoomScale = savedInstanceState?.getFloat(
      STATE_ZOOM_SCALE,
      LineByLineZoomLayout.MIN_ZOOM
    ) ?: LineByLineZoomLayout.MIN_ZOOM
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val context = requireContext()
    return LineByLineZoomLayout(context).also { layout ->
      layout.setContent(
        QuranLineByLineWrapperView(
          context,
          currentPage(),
          forcePageFit = arguments?.getBoolean(PARAM_FORCE_PAGE_FIT, false) == true
        )
      )
      layout.setZoomScale(zoomScale)
      zoomLayout = layout
    }
  }

  override fun onDestroyView() {
    zoomScale = resolveCurrentZoomScale(zoomScale, zoomLayout?.currentZoomScale())
    zoomLayout = null
    super.onDestroyView()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    zoomScale = resolveCurrentZoomScale(zoomScale, zoomLayout?.currentZoomScale())
    outState.putFloat(STATE_ZOOM_SCALE, zoomScale)
    super.onSaveInstanceState(outState)
  }

  fun zoomBy(delta: Float) {
    val currentScale = resolveCurrentZoomScale(zoomScale, zoomLayout?.currentZoomScale())
    val newScale = calculateZoomScale(currentScale, delta)
    zoomScale = newScale
    if (newScale == currentScale) {
      return
    }

    zoomLayout?.setZoomScale(zoomScale, animated = true)
  }

  private fun currentPage(): Int {
    return arguments?.getInt(PARAM_PAGE_NUMBER) ?: 1
  }

  companion object {
    private const val PARAM_PAGE_NUMBER = "pageNumber"
    private const val PARAM_FORCE_PAGE_FIT = "forcePageFit"
    private const val STATE_ZOOM_SCALE = "zoomScale"
    internal fun resolveCurrentZoomScale(savedScale: Float, layoutScale: Float?): Float {
      return layoutScale ?: savedScale
    }

    internal fun calculateZoomScale(currentScale: Float, delta: Float): Float {
      return LineByLineZoomLayout.calculateZoomScale(currentScale, delta)
    }

    fun newInstance(page: Int, forcePageFit: Boolean = false): QuranLineByLineFragment {
      val bundle = Bundle().apply {
        putInt(PARAM_PAGE_NUMBER, page)
        putBoolean(PARAM_FORCE_PAGE_FIT, forcePageFit)
      }
      return QuranLineByLineFragment().apply { arguments = bundle }
    }
  }
}
