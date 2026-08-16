package com.quran.labs.androidquran.database

import android.content.Context
import com.quran.data.dao.TranslationsDao
import com.quran.data.model.QuranText
import com.quran.data.model.VerseRange
import com.quran.labs.androidquran.data.QuranDataProvider
import com.quran.labs.androidquran.database.DatabaseHandler.TextType.Companion.ARABIC
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.mobile.di.qualifier.ApplicationContext
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationsDaoImpl @Inject constructor(
  @ApplicationContext private val appContext: Context,
  private val quranFileUtils: QuranFileUtils,
) : TranslationsDao {

  override suspend fun allAyahs(): List<QuranText> {
    return ayahs(VerseRange(1, 1, 114, 6, -1))
  }

  override suspend fun ayahs(verseRange: VerseRange): List<QuranText> {
    return withContext(Dispatchers.IO) {
      if (!quranFileUtils.hasArabicSearchDatabase()) {
        return@withContext emptyList()
      }
      val databaseHandler = DatabaseHandler.getDatabaseHandler(
        appContext, QuranDataProvider.QURAN_ARABIC_DATABASE, quranFileUtils)
      databaseHandler.getVerses(verseRange, ARABIC)
    }
  }
}
