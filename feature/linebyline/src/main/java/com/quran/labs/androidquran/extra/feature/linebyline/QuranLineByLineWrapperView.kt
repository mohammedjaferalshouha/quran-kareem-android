package com.quran.labs.androidquran.extra.feature.linebyline

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.FrameLayout
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quran.data.page.provider.di.inject
import com.quran.data.source.PageProvider
import com.quran.labs.androidquran.common.drawing.R
import com.quran.labs.androidquran.extra.feature.linebyline.model.MissingLineByLineImagesException
import com.quran.labs.androidquran.extra.feature.linebyline.presenter.QuranLineByLinePresenter
import com.quran.labs.androidquran.extra.feature.linebyline.resource.ImageBitmapUtil
import com.quran.labs.androidquran.extra.feature.linebyline.ui.QuranPageWrapper
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale

class QuranLineByLineWrapperView(
  context: Context,
  currentPage: Int,
  private val dualScreenMode: Boolean = false,
  private val forcePageFit: Boolean = false
) : FrameLayout(context) {
  @Volatile
  private var didRequestFallback = false

  @Inject
  lateinit var imageBitmapUtil: ImageBitmapUtil
  @Inject
  lateinit var quranLineByLinePresenter: QuranLineByLinePresenter
  @Inject
  lateinit var pageProvider: PageProvider

  init {
    inject(currentPage)
    addView(generateComposeView())
  }

  private fun generateComposeView(): ComposeView {
    val pageFlow = quranLineByLinePresenter.loadPage()
      .catch { throwable ->
        if (throwable is MissingLineByLineImagesException) {
          if (!didRequestFallback) {
            Timber.e(throwable, "Missing line image")
            withContext(Dispatchers.Main) {
              if (quranLineByLinePresenter.fallbackToImageType()) {
                didRequestFallback = true

                val activity = resolveActivity(context)
                finishAndRestart(activity)
              }
            }
          }

          emit(quranLineByLinePresenter.emptyState())
        } else {
          throw throwable
        }
      }

    return ComposeView(context).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      consumeWindowInsets = false
      setContent {
        val selectionEvents = remember { Channel<SelectionEvent>(Channel.UNLIMITED) }
        LaunchedEffect(selectionEvents) {
          for (event in selectionEvents) {
            when (event) {
              is SelectionEvent.Start -> quranLineByLinePresenter.startSelection(event.x, event.y)
              is SelectionEvent.Modify -> quranLineByLinePresenter.modifySelectionRange(
                event.offsetX,
                event.offsetY
              )
              SelectionEvent.End -> quranLineByLinePresenter.endSelection()
              is SelectionEvent.AccessibilitySelect ->
                quranLineByLinePresenter.selectAyahForAccessibility(event.sura, event.ayah)
            }
          }
        }
        val pageInfo = pageFlow.collectAsState(quranLineByLinePresenter.emptyState(), Dispatchers.IO)

        MaterialTheme {
          QuranPageWrapper(
            pageInfo = pageInfo.value,
            pageContentType = pageProvider.getPageContentType(),
            dualScreenMode = dualScreenMode,
            forcePageFit = forcePageFit,
            suraHeaderBitmap = imageBitmapUtil.alpha8Image(
              context.applicationContext,
              R.drawable.chapter_hdr
            ),
            ayahNumberFormatter = NumberFormat.getIntegerInstance(Locale("ar", "EG")),
            onClick = quranLineByLinePresenter::onClick,
            onPagePositioned = quranLineByLinePresenter::onPagePositioned,
            onSelectionStart = { x, y -> selectionEvents.trySend(SelectionEvent.Start(x, y)) },
            onSelectionModified = { offsetX, offsetY ->
              selectionEvents.trySend(SelectionEvent.Modify(offsetX, offsetY))
            },
            onSelectionEnd = { selectionEvents.trySend(SelectionEvent.End) },
            onAccessibilityAyahSelected = { sura, ayah ->
              selectionEvents.trySend(SelectionEvent.AccessibilitySelect(sura, ayah))
            }
          )
        }
      }
    }
  }

  private sealed interface SelectionEvent {
    data class Start(val x: Float, val y: Float) : SelectionEvent
    data class Modify(val offsetX: Float, val offsetY: Float) : SelectionEvent
    data object End : SelectionEvent
    data class AccessibilitySelect(val sura: Int, val ayah: Int) : SelectionEvent
  }

  private tailrec fun resolveActivity(context: Context): Activity? {
    return context as? Activity
        ?: if (context is ContextWrapper) {
          resolveActivity(context.baseContext)
        } else {
          null
        }
  }

  private fun finishAndRestart(activity: Activity?) {
    if (activity != null) {
      val restartIntent = Intent(activity.intent)
      activity.finish()
      activity.startActivity(restartIntent)
    }
  }
}
