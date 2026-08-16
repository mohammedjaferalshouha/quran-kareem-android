package com.quran.labs.androidquran.extra.feature.linebyline.model

import com.quran.mobile.linebyline.data.dao.AyahMarkerInfo
import com.quran.mobile.linebyline.data.dao.SuraHeader
import com.quran.data.model.QuranText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PageInfo(
  val page: Int,
  val pageType: String,
  val displayText: DisplayText,
  val displaySettings: DisplaySettings,
  val lineModels: ImmutableList<LineModel>,
  val suraHeaders: ImmutableList<SuraHeader>,
  val ayahMarkers: ImmutableList<AyahMarkerInfo>,
  val ayahHighlights: ImmutableList<HighlightAyah>,
  val sidelines: ImmutableList<SidelineModel>,
  val targetScrollPosition: Int,
  val showSidelines: Boolean,
  val skippedPageCount: Int,
  val accessibilityAyahs: ImmutableList<QuranText>
)

val EmptySettings =
  DisplaySettings(
    false,
    255,
    0,
    showHeaderFooter = false,
    showSidelines = false,
    showLineDividers = false
  )

val EmptyPageInfo =
  PageInfo(
    0,
    "",
    DisplayText("", "", "", "", ""),
    EmptySettings,
    persistentListOf(),
    persistentListOf(),
    persistentListOf(),
    persistentListOf(),
    persistentListOf(),
    -1,
    false,
    0,
    persistentListOf()
  )

internal fun buildAyahAccessibilityText(ayah: QuranText): String {
  return "${ayah.text} ۝${ayah.ayah}"
}
