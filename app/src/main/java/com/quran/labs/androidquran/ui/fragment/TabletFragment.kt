package com.quran.labs.androidquran.ui.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.data.model.selection.AyahSelection
import com.quran.labs.androidquran.common.QuranAyahInfo
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.extra.feature.linebyline.LineByLineZoomLayout
import com.quran.labs.androidquran.presenter.quran.QuranPagePresenter
import com.quran.labs.androidquran.presenter.quran.QuranPageScreen
import com.quran.labs.androidquran.presenter.quran.ayahtracker.AyahImageTrackerItem
import com.quran.labs.androidquran.presenter.quran.ayahtracker.AyahTrackerItem
import com.quran.labs.androidquran.presenter.quran.ayahtracker.AyahTrackerPresenter
import com.quran.labs.androidquran.presenter.quran.ayahtracker.AyahTrackerPresenter.AyahInteractionHandler
import com.quran.labs.androidquran.presenter.quran.ayahtracker.AyahTranslationTrackerItem
import com.quran.labs.androidquran.presenter.quran.ayahtracker.NoOpImageTrackerItem
import com.quran.labs.androidquran.presenter.translation.TranslationPresenter
import com.quran.labs.androidquran.ui.PagerActivity
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener
import com.quran.labs.androidquran.ui.helpers.AyahTracker
import com.quran.labs.androidquran.ui.helpers.QuranPage
import com.quran.labs.androidquran.ui.translation.TranslationView
import com.quran.labs.androidquran.ui.util.PageController
import com.quran.labs.androidquran.util.QuranScreenInfo
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.view.HighlightingImageView
import com.quran.labs.androidquran.view.QuranCustomImagePageLayout
import com.quran.labs.androidquran.view.QuranImagePageLayout
import com.quran.labs.androidquran.view.QuranTranslationPageLayout
import com.quran.labs.androidquran.view.TabletView
import com.quran.mobile.translation.model.LocalTranslation
import com.quran.page.common.data.AyahCoordinates
import com.quran.page.common.data.PageCoordinates
import com.quran.page.common.draw.ImageDrawHelper
import com.quran.page.common.factory.PageViewFactory
import com.quran.page.common.factory.PageViewFactoryProvider
import com.quran.reading.common.ReadingEventPresenter
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class TabletFragment : Fragment(), PageController, TranslationPresenter.TranslationScreen,
  QuranPage, QuranPageScreen, AyahInteractionHandler {
  object Mode {
    const val ARABIC = 1
    const val TRANSLATION = 2
  }

  private var mode = 0
  private var pageNumber = 0
  private var translationScrollPosition = 0
  private var splitTranslationScrollPosition = 0
  private var splitTranslationScrollOffset = 0
  private var splitReaderScrollPosition = 0
  private var splitTranslationExpansionState: LongArray? = null
  private var splitTranslationFootnoteState: LongArray? = null
  private var ayahCoordinatesError = false
  private var isSplitScreen = false
  private var splitReaderIsText = true
  private var leftTranslation: TranslationView? = null
  private var rightTranslation: TranslationView? = null
  private var leftImageView: HighlightingImageView? = null
  private var rightImageView: HighlightingImageView? = null
  private val compositeDisposable = CompositeDisposable()
  private var ayahTrackerItemsStorage: Array<AyahTrackerItem>? = null
  private var splitTranslationView: TranslationView? = null
  private var splitQuranTextView: TranslationView? = null
  private var pendingVerticalPageStart: Boolean? = null
  private var lastLongPressPage = -1

  private lateinit var mainView: TabletView

  @Inject lateinit var quranSettings: QuranSettings
  @Inject lateinit var ayahTrackerPresenter: AyahTrackerPresenter
  @Inject lateinit var quranPagePresenter: Lazy<QuranPagePresenter>
  @Inject lateinit var translationPresenter: Lazy<TranslationPresenter>
  @Inject lateinit var quranScreenInfo: QuranScreenInfo
  @Inject lateinit var quranInfo: QuranInfo
  @Inject lateinit var quranDisplayData: QuranDisplayData
  @Inject lateinit var imageDrawHelpers: Set<@JvmSuppressWildcards ImageDrawHelper>
  @Inject lateinit var readingEventPresenter: ReadingEventPresenter
  @Inject lateinit var pageProviderFactoryProvider: PageViewFactoryProvider

  private var pageViewFactory: PageViewFactory? = null
  private var isCustomArabicPageType = false

  private val scope = MainScope()

  override fun zoomBy(delta: Float) {
    if (!::mainView.isInitialized) {
      return
    }
    (mainView.leftPage as? QuranCustomImagePageLayout)
      ?.getWrappedView()
      ?.let { it as? LineByLineZoomLayout }
      ?.zoomBy(delta)
    (mainView.rightPage as? QuranCustomImagePageLayout)
      ?.getWrappedView()
      ?.let { it as? LineByLineZoomLayout }
      ?.zoomBy(delta)

    if (mode == Mode.TRANSLATION) {
      val leftPage = mainView.leftPage as? QuranTranslationPageLayout
      val rightPage = mainView.rightPage as? QuranTranslationPageLayout
      val sourcePage = rightPage ?: leftPage
      val changed = sourcePage?.zoomBy(delta, quranSettings) == true
      if (changed) {
        if (leftPage !== sourcePage) {
          leftPage?.translationView?.refresh(quranSettings)
        }
        if (rightPage !== sourcePage) {
          rightPage?.translationView?.refresh(quranSettings)
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      translationScrollPosition = savedInstanceState.getInt(
        SI_RIGHT_TRANSLATION_SCROLL_POSITION
      )
      splitTranslationScrollPosition = savedInstanceState.getInt(
        SI_SPLIT_TRANSLATION_SCROLL_POSITION
      )
      splitTranslationScrollOffset = savedInstanceState.getInt(
        SI_SPLIT_TRANSLATION_SCROLL_OFFSET
      )
      splitReaderScrollPosition = savedInstanceState.getInt(
        SI_SPLIT_READER_SCROLL_POSITION
      )
      splitTranslationExpansionState = savedInstanceState.getLongArray(
        SI_SPLIT_TRANSLATION_EXPANSION_STATE
      )
      splitTranslationFootnoteState = savedInstanceState.getLongArray(
        SI_SPLIT_TRANSLATION_FOOTNOTE_STATE
      )
    }
    pageViewFactory =
      pageProviderFactoryProvider.providePageViewFactory(quranSettings.pageType)
    isCustomArabicPageType = pageViewFactory != null
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    val context: Context? = activity
    mainView = TabletView(context)
    if (mode == Mode.ARABIC) {
      mainView.init(
        TabletView.QURAN_PAGE,
        TabletView.QURAN_PAGE,
        pageViewFactory,
        pageNumber + 1,
        pageNumber
      )
      if (mainView.leftPage is QuranImagePageLayout) {
        leftImageView = (mainView.leftPage as QuranImagePageLayout).getImageView()
        rightImageView = (mainView.rightPage as QuranImagePageLayout).getImageView()
      }
      mainView.setPageController(this, pageNumber + 1, pageNumber, quranInfo.skip)
    } else if (mode == Mode.TRANSLATION) {
      if (!isSplitScreen) {
        mainView.init(
          TabletView.TRANSLATION_PAGE,
          TabletView.TRANSLATION_PAGE,
          pageViewFactory,
          pageNumber + 1,
          pageNumber
        )
        val leftTranslation = (mainView.leftPage as QuranTranslationPageLayout).translationView
        val rightTranslation = (mainView.rightPage as QuranTranslationPageLayout).translationView
        leftTranslation.setPageNumber(pageNumber + 1)
        rightTranslation.setPageNumber(pageNumber)
        val pagerActivity = context as PagerActivity
        leftTranslation.setTranslationClickedListener { pagerActivity.toggleActionBar() }
        rightTranslation.setTranslationClickedListener { pagerActivity.toggleActionBar() }
        this.leftTranslation = leftTranslation
        this.rightTranslation = rightTranslation
        mainView.setPageController(this, pageNumber + 1, pageNumber, quranInfo.skip)
      } else {
        initSplitMode()
      }
    }
    return mainView
  }

  private fun initSplitMode() {
    if (splitReaderIsText) {
      mainView.init(
        TabletView.TRANSLATION_PAGE,
        TabletView.TRANSLATION_PAGE,
        null,
        pageNumber,
        pageNumber
      )
      splitQuranTextView =
        (mainView.rightPage as QuranTranslationPageLayout).translationView.apply {
          setDisplayMode(TranslationView.DisplayMode.QURAN_TEXT)
          setPageNumber(pageNumber)
        }
    } else {
      mainView.init(
        TabletView.TRANSLATION_PAGE,
        TabletView.QURAN_PAGE,
        pageViewFactory,
        pageNumber,
        pageNumber
      )
      (mainView.rightPage as? QuranImagePageLayout)?.let { quranPage ->
        rightImageView = quranPage.getImageView()
      }
    }
    splitTranslationView =
      (mainView.leftPage as QuranTranslationPageLayout).translationView.apply {
        setDisplayMode(TranslationView.DisplayMode.TRANSLATION_ONLY)
        setPageNumber(pageNumber)
        setIsolateTranslationScrollFromPager(true)
        restoreExpandedTafseerState(splitTranslationExpansionState)
        restoreExpandedFootnoteState(splitTranslationFootnoteState)
      }

    val pagerActivity = activity as PagerActivity
    splitTranslationView?.setTranslationClickedListener { pagerActivity.toggleActionBar() }
    splitQuranTextView?.setTranslationClickedListener { pagerActivity.toggleActionBar() }
    splitQuranTextView?.setReadingPositionChangedListener { ayahInfo ->
      pagerActivity.rememberReadingPosition(pageNumber, ayahInfo.sura, ayahInfo.ayah, true)
    }
    mainView.setPageController(this, pageNumber, quranInfo.skip)
  }

  override fun onStart() {
    super.onStart()
    ayahTrackerPresenter.bind(this)
    if (mode == Mode.ARABIC) {
      if (!isCustomArabicPageType) {
        quranPagePresenter.value.bind(this)
      }
    } else {
      translationPresenter.value.bind(this)
      if (isSplitScreen && !splitReaderIsText && !isCustomArabicPageType) {
        quranPagePresenter.value.bind(this)
      }
    }
  }

  override fun onPause() {
    if (mode == Mode.TRANSLATION) {
      val rightTranslation = rightTranslation
      if (isSplitScreen) {
        splitTranslationView?.let {
          val anchor = it.translationScrollAnchor
          splitTranslationScrollPosition = anchor[0]
          splitTranslationScrollOffset = anchor[1]
        }
        if (splitReaderIsText) {
          splitQuranTextView?.let {
            splitReaderScrollPosition = it.findFirstCompletelyVisibleItemPosition()
          }
        }
      } else if (rightTranslation != null) {
        translationScrollPosition = rightTranslation.findFirstCompletelyVisibleItemPosition()
      }
    }
    super.onPause()
  }

  override fun onStop() {
    ayahTrackerPresenter.unbind(this)
    if (mode == Mode.ARABIC) {
      quranPagePresenter.value.unbind(this)
    } else {
      translationPresenter.value.unbind(this)
      if (isSplitScreen && !splitReaderIsText && !isCustomArabicPageType) {
        quranPagePresenter.value.unbind(this)
      }
    }
    super.onStop()
  }

  override fun onResume() {
    super.onResume()
    updateView()
    if (mode == Mode.TRANSLATION) {
      if (isSplitScreen) {
        splitTranslationView?.refresh(quranSettings)
        splitQuranTextView?.refresh(quranSettings)
      } else {
        rightTranslation?.refresh(quranSettings)
        leftTranslation?.refresh(quranSettings)
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    if (mode == Mode.TRANSLATION) {
      if (isSplitScreen) {
        splitTranslationView?.let {
          val anchor = it.translationScrollAnchor
          outState.putInt(
            SI_SPLIT_TRANSLATION_SCROLL_POSITION,
            anchor[0]
          )
          outState.putInt(
            SI_SPLIT_TRANSLATION_SCROLL_OFFSET,
            anchor[1]
          )
          outState.putLongArray(
            SI_SPLIT_TRANSLATION_EXPANSION_STATE,
            it.expandedTafseerState
          )
          outState.putLongArray(
            SI_SPLIT_TRANSLATION_FOOTNOTE_STATE,
            it.expandedFootnoteState
          )
        }
        if (splitReaderIsText) {
          splitQuranTextView?.let {
            outState.putInt(
              SI_SPLIT_READER_SCROLL_POSITION,
              it.findFirstCompletelyVisibleItemPosition()
            )
          }
        }
      } else {
        outState.putInt(
          SI_RIGHT_TRANSLATION_SCROLL_POSITION,
          rightTranslation!!.findFirstCompletelyVisibleItemPosition()
        )
      }
    }
    super.onSaveInstanceState(outState)
  }

  override fun updateView() {
    if (isAdded) {
      mainView.updateView(quranSettings)
    }
  }

  override fun getAyahTracker(): AyahTracker {
    return ayahTrackerPresenter
  }

  override fun getAyahTrackerItems(): Array<AyahTrackerItem> {
    val cachedTrackerItems = ayahTrackerItemsStorage
    return if (cachedTrackerItems == null) {
      if (mode == Mode.ARABIC) {
        val leftImageView = leftImageView
        val rightImageView = rightImageView
        if (leftImageView != null && rightImageView != null) {
          val left = if (quranInfo.isValidPage(pageNumber + 1)) {
            AyahImageTrackerItem(
              pageNumber + 1,
              quranInfo,
              quranDisplayData,
              false,
              imageDrawHelpers,
              leftImageView
            )
          } else {
            NoOpImageTrackerItem(pageNumber + 1)
          }

          val right = AyahImageTrackerItem(
            pageNumber,
            quranInfo,
            quranDisplayData,
            true,
            imageDrawHelpers,
            rightImageView
          )
          arrayOf(right, left)
        } else {
          emptyArray()
        }
      } else if (mode == Mode.TRANSLATION) {
        if (isSplitScreen) {
          val translationItem =
            AyahTranslationTrackerItem(pageNumber, quranInfo, splitTranslationView!!)
          if (splitReaderIsText) {
            val quranTextItem =
              AyahTranslationTrackerItem(pageNumber, quranInfo, splitQuranTextView!!)
            arrayOf(quranTextItem, translationItem)
          } else {
            val imageView = rightImageView
            if (imageView != null) {
              val imageItem = AyahImageTrackerItem(
                pageNumber,
                quranInfo,
                quranDisplayData,
                true,
                imageDrawHelpers,
                imageView
              )
              arrayOf(imageItem, translationItem)
            } else {
              arrayOf(translationItem)
            }
          }
        } else {
          val left = AyahTranslationTrackerItem(pageNumber + 1, quranInfo, leftTranslation!!)
          val right = AyahTranslationTrackerItem(pageNumber, quranInfo, rightTranslation!!)
          arrayOf(right, left)
        }
      } else {
        emptyArray()
      }
    } else {
      cachedTrackerItems
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    val arguments = requireArguments()
    pageNumber = arguments.getInt(FIRST_PAGE_EXTRA)
    mode = arguments.getInt(MODE_EXTRA, Mode.ARABIC)
    isSplitScreen = arguments.getBoolean(IS_SPLIT_SCREEN, false)
    splitReaderIsText = arguments.getBoolean(SPLIT_READER_IS_TEXT, true)
    val pages =
      if (isSplitScreen && mode == Mode.TRANSLATION) intArrayOf(pageNumber) else intArrayOf(
        pageNumber,
        pageNumber + 1
      )
    (activity as PagerActivity).pagerActivityComponent
      .quranPageComponentFactory()
      .generate(pages)
      .inject(this)
  }

  override fun onDetach() {
    compositeDisposable.clear()
    scope.cancel()
    super.onDetach()
  }

  override fun setPageDownloadError(@StringRes errorMessage: Int) {
    mainView.showError(errorMessage)
    mainView.setOnClickListener { ayahTrackerPresenter.onPressIgnoringSelectionState() }
  }

  override fun setPageBitmap(page: Int, pageBitmap: Bitmap) {
    val imageView: ImageView? = if (page == pageNumber) rightImageView else leftImageView
    imageView?.setImageDrawable(BitmapDrawable(resources, pageBitmap))
  }

  override fun hidePageDownloadError() {
    mainView.hideError()
    mainView.setOnClickListener(null)
    mainView.isClickable = false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (mode == Mode.TRANSLATION) {
      scope.launch {
        translationPresenter.value.refresh()
      }
    }
  }

  override fun setVerses(
    page: Int,
    translations: Array<LocalTranslation>,
    verses: List<QuranAyahInfo>
  ) {
    if (isSplitScreen) {
      splitTranslationView?.setPageNumber(page)
      splitTranslationView?.setVerses(quranDisplayData, translations, verses)
      if (splitReaderIsText) {
        splitQuranTextView?.setPageNumber(page)
        splitQuranTextView?.setVerses(quranDisplayData, emptyArray(), verses)
      }
    } else {
      if (page == pageNumber) {
        rightTranslation?.setPageNumber(page)
        rightTranslation?.setVerses(quranDisplayData, translations, verses)
      } else if (page == pageNumber + 1) {
        leftTranslation?.setPageNumber(page)
        leftTranslation?.setVerses(quranDisplayData, translations, verses)
      }
    }
  }

  override fun updateScrollPosition() {
    if (isSplitScreen) {
      splitTranslationView?.restoreTranslationScrollAnchor(
        splitTranslationScrollPosition,
        splitTranslationScrollOffset
      )
      if (splitReaderIsText) {
        splitQuranTextView?.setScrollPosition(splitReaderScrollPosition)
        applyPendingVerticalPageEntry()
      }
    } else {
      rightTranslation?.setScrollPosition(translationScrollPosition)
    }
  }

  fun refresh() {
    if (mode == Mode.TRANSLATION) {
      scope.launch {
        translationPresenter.value.refresh()
      }
    }
  }

  override fun onArabicTextUnavailable() {
    (activity as? PagerActivity)?.onArabicTextUnavailable(pageNumber)
  }

  override val shouldLoadTranslations: Boolean
    get() = isSplitScreen

  fun cleanup() {
    Timber.d("cleaning up page %d", pageNumber)
    leftImageView?.setImageDrawable(null)
    rightImageView?.setImageDrawable(null)
  }

  override fun setPageCoordinates(pageCoordinates: PageCoordinates) {
    ayahTrackerPresenter.setPageBounds(pageCoordinates)
  }

  override fun setAyahCoordinatesError() {
    ayahCoordinatesError = true
  }

  override fun setAyahCoordinatesData(coordinates: AyahCoordinates) {
    ayahTrackerPresenter.setAyahCoordinates(coordinates)
  }

  override fun handleTouchEvent(
    event: MotionEvent,
    eventType: AyahSelectedListener.EventType,
    page: Int
  ): Boolean {
    return isVisible && ayahTrackerPresenter.handleTouchEvent(
      requireActivity(), event, eventType,
      page, ayahCoordinatesError
    )
  }

  override fun handleLongPress(suraAyah: SuraAyah) {
    if (isVisible) {
      val page = quranInfo.getPageFromSuraAyah(suraAyah.sura, suraAyah.ayah)
      if (page != lastLongPressPage) {
        ayahTrackerPresenter.endAyahMode()
      }
      lastLongPressPage = page
      ayahTrackerPresenter.onLongPress(suraAyah)
    }
  }

  override fun handleRetryClicked() {
    hidePageDownloadError()
    quranPagePresenter.value.downloadImages()
  }

  override fun onScrollChanged(y: Float) {
    if (isVisible) {
      val views = arrayOf(rightTranslation, leftTranslation)
      for (view in views) {
        if (view != null) {
          val ayahSelection = readingEventPresenter.currentAyahSelection()
          if (ayahSelection is AyahSelection.Ayah) {
            val (suraAyah) = ayahSelection
            readingEventPresenter.onAyahSelection(
              AyahSelection.Ayah(
                suraAyah,
                view.getToolbarPosition(suraAyah.sura, suraAyah.ayah)
              )
            )
          }
        }
      }
    }
  }

  override fun endAyahMode() {
    if (isVisible) {
      ayahTrackerPresenter.endAyahMode()
    }
  }

  override fun isVerticalPageNavigationEnabled(): Boolean {
    return (activity as? PagerActivity)?.isVerticalPageNavigationEnabled() == true
  }

  override fun requestAdjacentVerticalPage(forward: Boolean) {
    (activity as? PagerActivity)?.moveToAdjacentVerticalPage(forward)
  }

  fun prepareVerticalPageEntry(start: Boolean) {
    pendingVerticalPageStart = start
    applyPendingVerticalPageEntry()
  }

  private fun applyPendingVerticalPageEntry() {
    val start = pendingVerticalPageStart ?: return
    if (!isSplitScreen || !splitReaderIsText) {
      return
    }
    val textView = splitQuranTextView ?: return
    if (!textView.isFlowingQuranTextReady) {
      return
    }
    pendingVerticalPageStart = null
    if (start) {
      textView.scrollToPageStart()
    } else {
      textView.scrollToPageEnd()
    }
  }

  companion object {
    private const val FIRST_PAGE_EXTRA = "pageNumber"
    private const val MODE_EXTRA = "mode"
    private const val IS_SPLIT_SCREEN = "splitScreenMode"
    private const val SPLIT_READER_IS_TEXT = "splitReaderIsText"
    private const val SI_RIGHT_TRANSLATION_SCROLL_POSITION =
      "SI_RIGHT_TRANSLATION_SCROLL_POSITION"
    private const val SI_SPLIT_TRANSLATION_SCROLL_POSITION =
      "SI_SPLIT_TRANSLATION_SCROLL_POSITION"
    private const val SI_SPLIT_TRANSLATION_SCROLL_OFFSET =
      "SI_SPLIT_TRANSLATION_SCROLL_OFFSET"
    private const val SI_SPLIT_READER_SCROLL_POSITION =
      "SI_SPLIT_READER_SCROLL_POSITION"
    private const val SI_SPLIT_TRANSLATION_EXPANSION_STATE =
      "SI_SPLIT_TRANSLATION_EXPANSION_STATE"
    private const val SI_SPLIT_TRANSLATION_FOOTNOTE_STATE =
      "SI_SPLIT_TRANSLATION_FOOTNOTE_STATE"

    fun newInstance(
      firstPage: Int,
      mode: Int,
      isSplitScreen: Boolean,
      splitReaderIsText: Boolean = true
    ): TabletFragment {
      val f = TabletFragment()
      val args = Bundle()
      args.putInt(FIRST_PAGE_EXTRA, firstPage)
      args.putInt(MODE_EXTRA, mode)
      args.putBoolean(IS_SPLIT_SCREEN, isSplitScreen)
      args.putBoolean(SPLIT_READER_IS_TEXT, splitReaderIsText)
      f.setArguments(args)
      return f
    }
  }
}
