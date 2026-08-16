package com.quran.labs.androidquran.ui.translation;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.quran.labs.androidquran.base.TestApplication;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.view.QuranTranslationPageLayout;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(application = TestApplication.class, sdk = 33)
@RunWith(RobolectricTestRunner.class)
public class TranslationViewTest {

  @After
  public void tearDown() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranSettings settings = QuranSettings.getInstance(context);
    settings.setNightMode(false);
    settings.setAyahTextSize(15);
    QuranSettings.setInstance(null);
  }

  @Test
  public void textPageClipsItsContentToPageBounds() {
    final Context context = ApplicationProvider.getApplicationContext();
    final TranslationView view = new TranslationView(context);

    assertThat(view.getClipChildren()).isTrue();
    assertThat(view.getClipToPadding()).isTrue();

    final ViewGroup textScroll = (ViewGroup) view.getChildAt(1);
    assertThat(textScroll.getClipChildren()).isTrue();
    assertThat(textScroll.getClipToPadding()).isTrue();
  }

  @Test
  public void refreshUsesOpaqueDaySurfaceForTextPage() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranSettings settings = QuranSettings.getInstance(context);
    settings.setNightMode(false);
    final TranslationView view = new TranslationView(context);

    view.refresh(settings);

    assertOpaqueSurface(view, Color.WHITE);
  }

  @Test
  public void refreshUsesConfiguredOpaqueNightSurfaceForTextPage() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranSettings settings = QuranSettings.getInstance(context);
    settings.setNightMode(true);
    final int brightness = settings.getNightModeBackgroundBrightness();
    final TranslationView view = new TranslationView(context);

    view.refresh(settings);

    assertOpaqueSurface(view, Color.rgb(brightness, brightness, brightness));
  }

  @Test
  public void translationPageZoomPersistsAndRefreshesBothTabletSides() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranSettings settings = QuranSettings.getInstance(context);
    settings.setAyahTextSize(15);
    final QuranTranslationPageLayout rightPage = new QuranTranslationPageLayout(context);
    final QuranTranslationPageLayout leftPage = new QuranTranslationPageLayout(context);

    assertThat(rightPage.zoomBy(1f, settings)).isTrue();
    leftPage.getTranslationView().refresh(settings);

    assertThat(settings.getAyahTextSize()).isEqualTo(18);
    assertThat(quranTextSize(rightPage.getTranslationView()))
        .isEqualTo(quranTextSize(leftPage.getTranslationView()));
    assertThat(quranTextSize(rightPage.getTranslationView())).isGreaterThan(15f);
  }

  @Test
  public void translationPageZoomStopsAtConfiguredBounds() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranSettings settings = QuranSettings.getInstance(context);
    final QuranTranslationPageLayout page = new QuranTranslationPageLayout(context);
    settings.setAyahTextSize(60);

    assertThat(page.zoomBy(1f, settings)).isFalse();
    assertThat(settings.getAyahTextSize()).isEqualTo(60);

    settings.setAyahTextSize(9);
    assertThat(page.zoomBy(-1f, settings)).isFalse();
    assertThat(settings.getAyahTextSize()).isEqualTo(9);
  }

  @Test
  public void reachingTheEndDuringAnExistingGestureDoesNotTurnThePage() {
    assertThat(TranslationView.shouldRequestBoundaryPage(
        false, false, 1, 120f, 20f)).isFalse();
  }

  @Test
  public void aNewGestureAtTheBottomTurnsForwardOncePastTheThreshold() {
    assertThat(TranslationView.shouldRequestBoundaryPage(
        false, true, 1, 120f, 20f)).isTrue();
    assertThat(TranslationView.shouldRequestBoundaryPage(
        false, true, 1, 10f, 20f)).isFalse();
  }

  @Test
  public void aNewGestureAtTheTopTurnsBackwardOncePastTheThreshold() {
    assertThat(TranslationView.shouldRequestBoundaryPage(
        true, false, -1, 120f, 20f)).isTrue();
    assertThat(TranslationView.shouldRequestBoundaryPage(
        true, false, 1, 120f, 20f)).isFalse();
  }

  private static float quranTextSize(TranslationView view) {
    final ViewGroup textScroll = (ViewGroup) view.getChildAt(1);
    return ((TextView) textScroll.getChildAt(0)).getTextSize();
  }

  private static void assertOpaqueSurface(TranslationView view, int expectedColor) {
    assertColor(view, expectedColor);
    final ViewGroup textScroll = (ViewGroup) view.getChildAt(1);
    assertColor(textScroll, expectedColor);
    assertColor(textScroll.getChildAt(0), expectedColor);
    assertThat(Color.alpha(expectedColor)).isEqualTo(255);
  }

  private static void assertColor(View view, int expectedColor) {
    assertThat(view.getBackground()).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) view.getBackground()).getColor()).isEqualTo(expectedColor);
  }
}
