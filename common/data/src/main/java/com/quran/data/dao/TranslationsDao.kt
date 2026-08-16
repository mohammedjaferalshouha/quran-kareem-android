package com.quran.data.dao

import com.quran.data.model.QuranText
import com.quran.data.model.VerseRange

interface TranslationsDao {
  suspend fun allAyahs(): List<QuranText>
  suspend fun ayahs(verseRange: VerseRange): List<QuranText>
}
