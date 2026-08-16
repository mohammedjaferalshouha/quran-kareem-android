package com.quran.labs.androidquran.ui.helpers;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.text.TextPaint;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.base.TestApplication;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(application = TestApplication.class, sdk = 33)
@RunWith(RobolectricTestRunner.class)
public class SuraHeaderSpanTest {

  @Test
  public void suraHeaderUsesTheMushafGreenResource() {
    final Context context = ApplicationProvider.getApplicationContext();
    final TextPaint paint = new TextPaint();

    new SuraHeaderSpan(context).updateDrawState(paint);

    assertThat(paint.getColor()).isEqualTo(
        ContextCompat.getColor(context, R.color.quran_text_sura_header));
  }
}
