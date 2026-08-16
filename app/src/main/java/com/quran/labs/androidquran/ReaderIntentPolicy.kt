package com.quran.labs.androidquran

import android.content.Intent
import com.quran.labs.androidquran.ui.PagerActivity

internal fun readerReuseFlags(): Int {
  return Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}

internal fun Intent.prepareForReaderReuse(jumpToTranslation: Boolean): Intent {
  addFlags(readerReuseFlags())
  putExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, jumpToTranslation)
  return this
}

internal fun Intent.explicitReaderMode(): Boolean? {
  return if (hasExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION)) {
    getBooleanExtra(PagerActivity.EXTRA_JUMP_TO_TRANSLATION, false)
  } else {
    null
  }
}
