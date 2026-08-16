package com.quran.labs.androidquran.view;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.quran.labs.androidquran.base.TestApplication;
import com.quran.labs.androidquran.util.QuranSettings;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(application = TestApplication.class, sdk = 33)
@RunWith(RobolectricTestRunner.class)
public class QuranTranslationPageLayoutTest {

  @After
  public void tearDown() {
    final Context context = ApplicationProvider.getApplicationContext();
    QuranSettings.getInstance(context).setVerticalPageNavigation(false);
    QuranSettings.setInstance(null);
  }

  @Test
  public void verticalTextPageClipsContentAndHidesBookBorders() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranSettings settings = QuranSettings.getInstance(context);
    settings.setVerticalPageNavigation(true);
    final TestLayout layout = new TestLayout(context);

    assertThat(layout.getClipChildren()).isTrue();
    assertThat(layout.getClipToPadding()).isTrue();
    assertThat(layout.drawsBookBorders(settings)).isFalse();
  }

  private static final class TestLayout extends QuranTranslationPageLayout {
    TestLayout(Context context) {
      super(context);
    }

    boolean drawsBookBorders(QuranSettings settings) {
      return shouldDrawBookBorders(settings);
    }
  }
}
