package com.quran.labs.androidquran.ui.helpers;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.quran.labs.androidquran.R;

/** Makes sura headers slightly larger, heavier, and clearer in both themes. */
public final class SuraHeaderSpan extends MetricAffectingSpan {
  private static final float SIZE_RATIO = 0.90f;

  private final Context appContext;

  public SuraHeaderSpan(@NonNull Context context) {
    appContext = context.getApplicationContext();
  }

  @Override
  public void updateDrawState(@NonNull TextPaint textPaint) {
    applyTextStyle(textPaint);
    textPaint.setColor(ContextCompat.getColor(appContext, R.color.quran_text_sura_header));
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
