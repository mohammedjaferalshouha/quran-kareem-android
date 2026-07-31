package com.quran.labs.androidquran.ui.helpers;

import android.content.Context;
import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

import com.quran.labs.androidquran.util.QuranSettings;

/** Makes sura headers slightly larger, heavier, and clearer in both themes. */
public final class SuraHeaderSpan extends MetricAffectingSpan {
  private static final float SIZE_RATIO = 0.90f;

  private final QuranSettings quranSettings;

  public SuraHeaderSpan(@NonNull Context context) {
    quranSettings = QuranSettings.getInstance(context.getApplicationContext());
  }

  @Override
  public void updateDrawState(@NonNull TextPaint textPaint) {
    applyTextStyle(textPaint);
    if (quranSettings.isNightMode()) {
      final int brightness = quranSettings.getNightModeTextBrightness();
      textPaint.setColor(Color.rgb(brightness, brightness, brightness));
    } else {
      textPaint.setColor(Color.BLACK);
    }
  }

  @Override
  public void updateMeasureState(@NonNull TextPaint textPaint) {
    applyTextStyle(textPaint);
  }

  private void applyTextStyle(@NonNull TextPaint textPaint) {
    textPaint.setTextSize(textPaint.getTextSize() * SIZE_RATIO);
    textPaint.setFakeBoldText(true);
  }
}
