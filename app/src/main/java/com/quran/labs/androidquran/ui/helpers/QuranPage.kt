package com.quran.labs.androidquran.ui.helpers

interface QuranPage {
  fun updateView()
  fun getAyahTracker(): AyahTracker
  fun zoomBy(delta: Float) = Unit
}
