package com.quran.labs.androidquran.pageselect

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager.widget.ViewPager
import com.quran.data.core.QuranInfo
import com.quran.data.source.PageProvider
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.QuranDataActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.ui.helpers.QuranDisplayHelper
import com.quran.labs.androidquran.util.QuranSettings
import dev.zacsweers.metro.Inject

class PageSelectActivity : AppCompatActivity() {
  @Inject lateinit var presenter : PageSelectPresenter
  @Inject lateinit var quranSettings: QuranSettings
  @Inject lateinit var pageTypes: Map<@JvmSuppressWildcards String, @JvmSuppressWildcards PageProvider>

  private lateinit var adapter : PageSelectAdapter
  private lateinit var viewPager: ViewPager

  private var isProcessing = false

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()

    super.onCreate(savedInstanceState)
    (application as QuranApplication).applicationComponent.inject(this)

    setContentView(R.layout.page_select)

    val display = windowManager.defaultDisplay
    val width = QuranDisplayHelper.getWidthKitKat(display)

    adapter = PageSelectAdapter(LayoutInflater.from(this), width) {
      onPageTypeSelected(it)
    }

    viewPager = findViewById(R.id.pager)
    viewPager.adapter = adapter

    ViewCompat.setOnApplyWindowInsetsListener(viewPager) { _, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
      )
      viewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = insets.top
        bottomMargin = insets.bottom
        leftMargin = insets.left
        rightMargin = insets.right
      }

      windowInsets
    }

    // let the next and previous pages be slightly visible
    val pageMargin = resources.getDimensionPixelSize(R.dimen.page_margin)
    val pagerPadding = pageMargin * 2
    viewPager.setPadding(pagerPadding, 0, pagerPadding, 0)
    viewPager.clipToPadding = false
    viewPager.pageMargin = pageMargin
  }

  override fun onResume() {
    super.onResume()
    presenter.bind(this)
  }

  override fun onPause() {
    presenter.unbind(this)
    super.onPause()
  }

  override fun onDestroy() {
    adapter.cleanUp()
    super.onDestroy()
  }

  fun onUpdatedData(data: List<PageTypeItem>) {
    adapter.replaceItems(data, viewPager)
    adapter.positionOf(quranSettings.pageType)
      .takeIf { it >= 0 && it != viewPager.currentItem }
      ?.let { viewPager.setCurrentItem(it, false) }
  }

  private fun onPageTypeSelected(type: String) {
    val pageType = quranSettings.pageType
    if (pageType != type) {
      isProcessing = true
      val savedSura = quranSettings.automaticLastSura
      val savedAyah = quranSettings.automaticLastAyah
      val savedTextMode = quranSettings.wasAutomaticLastTextMode()
      val currentQuranInfo = pageTypes[pageType]?.getDataSource()?.let(::QuranInfo)
      val sourcePosition = resolveSourceReadingPosition(
        currentQuranInfo,
        quranSettings.automaticLastPage,
        savedSura,
        savedAyah
      )
      val selectedQuranInfo = pageTypes[type]?.getDataSource()?.let(::QuranInfo)
      val destination = resolvePageSelectionDestination(
        selectedQuranInfo,
        quranSettings.automaticLastPage,
        sourcePosition.sura,
        sourcePosition.ayah
      )

      // Bookmarks are backed by mobile-sync ayah records, so no page-number migration is needed.
      quranSettings.pageType = type

      // Reopen the reader through the data gate so the selected renderer and any
      // required page assets are refreshed immediately at the same ayah.
      val intent = QuranDataActivity.openPageIntent(
        context = this@PageSelectActivity,
        page = destination.page,
        sura = destination.sura,
        ayah = destination.ayah,
        jumpToTranslation = savedTextMode
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      startActivity(intent)
      finish()
    } else {
      finish()
    }
    isProcessing = false
  }
}

internal data class PageSelectionDestination(
  val page: Int,
  val sura: Int?,
  val ayah: Int?
)

internal data class SourceReadingPosition(val sura: Int, val ayah: Int)

internal fun resolveSourceReadingPosition(
  currentQuranInfo: QuranInfo?,
  savedPage: Int,
  savedSura: Int,
  savedAyah: Int
): SourceReadingPosition {
  if (savedSura > 0 && savedAyah > 0) {
    return SourceReadingPosition(savedSura, savedAyah)
  }
  if (currentQuranInfo != null && currentQuranInfo.isValidPage(savedPage)) {
    val bounds = currentQuranInfo.getPageBounds(savedPage)
    return SourceReadingPosition(bounds[0], bounds[1])
  }
  return SourceReadingPosition(-1, -1)
}

internal fun resolvePageSelectionDestination(
  selectedQuranInfo: QuranInfo?,
  savedPage: Int,
  savedSura: Int,
  savedAyah: Int
): PageSelectionDestination {
  if (selectedQuranInfo != null && savedSura > 0 && savedAyah > 0) {
    val mappedPage = selectedQuranInfo.getPageFromSuraAyah(savedSura, savedAyah)
    if (selectedQuranInfo.isValidPage(mappedPage)) {
      return PageSelectionDestination(mappedPage, savedSura, savedAyah)
    }
  }

  val fallbackPage = savedPage.takeIf { selectedQuranInfo?.isValidPage(it) == true }
    ?: com.quran.data.core.QuranConstants.PAGES_FIRST
  return PageSelectionDestination(fallbackPage, null, null)
}
