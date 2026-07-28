package com.quran.labs.androidquran.ui.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;

import com.quran.labs.androidquran.ui.util.TypefaceManager;

/**
 * Draws a page number as one atomic ornament.
 *
 * The Uthmani font has ready-made number ornaments for many ayah numbers, but not for every
 * mushaf page up to 604. Drawing the original empty ornament and centering the complete page
 * number inside it prevents Android from splitting unsupported numbers into multiple ornaments.
 */
public final class PageNumberSpan extends ReplacementSpan {
  private static final String ORNAMENT = "\u06DD";
  private static final float ORNAMENT_SCALE = 1.30f;
  private static final float NUMBER_SCALE = 0.36f;
  private static final float MAX_NUMBER_WIDTH_RATIO = 0.66f;

  private final Context context;
  private final Rect ornamentBounds = new Rect();
  private final Rect numberBounds = new Rect();

  public PageNumberSpan(@NonNull Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public int getSize(
      @NonNull Paint paint,
      CharSequence text,
      int start,
      int end,
      Paint.FontMetricsInt fontMetrics
  ) {
    final TextPaint ornamentPaint = ornamentPaint(paint);
    if (fontMetrics != null) {
      final Paint.FontMetricsInt ornamentMetrics = ornamentPaint.getFontMetricsInt();
      fontMetrics.ascent = ornamentMetrics.ascent;
      fontMetrics.descent = ornamentMetrics.descent;
      fontMetrics.top = ornamentMetrics.top;
      fontMetrics.bottom = ornamentMetrics.bottom;
      fontMetrics.leading = ornamentMetrics.leading;
    }
    return (int) Math.ceil(ornamentPaint.measureText(ORNAMENT));
  }

  @Override
  public void draw(
      @NonNull Canvas canvas,
      CharSequence text,
      int start,
      int end,
      float x,
      int top,
      int y,
      int bottom,
      @NonNull Paint paint
  ) {
    final String number = text.subSequence(start, end).toString();
    final TextPaint ornamentPaint = ornamentPaint(paint);
    final float width = ornamentPaint.measureText(ORNAMENT);
    canvas.drawText(ORNAMENT, x, y, ornamentPaint);

    final TextPaint numberPaint = new TextPaint(paint);
    numberPaint.setTypeface(android.graphics.Typeface.DEFAULT);
    numberPaint.setTextAlign(Paint.Align.CENTER);
    numberPaint.setTextSize(ornamentPaint.getTextSize() * NUMBER_SCALE);

    final float maximumNumberWidth = width * MAX_NUMBER_WIDTH_RATIO;
    final float measuredNumberWidth = numberPaint.measureText(number);
    if (measuredNumberWidth > maximumNumberWidth && measuredNumberWidth > 0f) {
      numberPaint.setTextSize(
          numberPaint.getTextSize() * maximumNumberWidth / measuredNumberWidth);
    }

    ornamentPaint.getTextBounds(ORNAMENT, 0, ORNAMENT.length(), ornamentBounds);
    numberPaint.getTextBounds(number, 0, number.length(), numberBounds);
    final float ornamentCenterY = y + (ornamentBounds.top + ornamentBounds.bottom) / 2f;
    final float numberBaseline =
        ornamentCenterY - (numberBounds.top + numberBounds.bottom) / 2f;
    canvas.drawText(number, x + width / 2f, numberBaseline, numberPaint);
  }

  private TextPaint ornamentPaint(@NonNull Paint source) {
    final TextPaint paint = new TextPaint(source);
    paint.setTypeface(TypefaceManager.INSTANCE.getUthmaniTypeface(context));
    paint.setTextSize(source.getTextSize() * ORNAMENT_SCALE);
    return paint;
  }
}
