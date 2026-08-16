package com.quran.labs.androidquran.extra.feature.linebyline

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import com.quran.page.common.data.PageMode
import com.quran.page.common.factory.PageViewFactory

class QuranLineByLinePageViewFactory(
  private val forcePageFit: () -> Boolean = { false }
) : PageViewFactory {
  override fun providePage(pageNumber: Int, pageMode: PageMode): Fragment? {
    return if (pageMode == PageMode.SingleArabicPage) {
      QuranLineByLineFragment.newInstance(pageNumber, forcePageFit())
    } else {
      null
    }
  }

  override fun providePageView(context: Context, pageNumber: Int, pageMode: PageMode): View? {
    return if (pageMode == PageMode.DualScreenMode.Arabic) {
      LineByLineZoomLayout(context).apply {
        setContent(QuranLineByLineWrapperView(context, pageNumber, dualScreenMode = true))
      }
    } else {
      null
    }
  }
}
