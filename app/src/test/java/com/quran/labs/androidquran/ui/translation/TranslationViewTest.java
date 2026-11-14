package com.quran.labs.androidquran.ui.translation;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.quran.data.core.QuranInfo;
import com.quran.data.model.SuraAyah;
import com.quran.labs.androidquran.base.TestApplication;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.common.TranslationMetadata;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource;
import com.quran.labs.androidquran.ui.helpers.HighlightTypes;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.view.QuranTranslationPageLayout;
import com.quran.mobile.translation.model.LocalTranslation;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

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

  @Test
  public void flowingQuranTextAppliesAndClearsAudioHighlightSpan() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranDisplayData displayData = new QuranDisplayData(new QuranInfo(new MadaniDataSource()));
    final TranslationView view = new TranslationView(context);
    final List<QuranAyahInfo> verses = Collections.singletonList(
        new QuranAyahInfo(1, 1, "بِسْمِ اللَّهِ", Collections.emptyList(), 1)
    );

    view.setDisplayMode(TranslationView.DisplayMode.QURAN_TEXT);
    view.setVerses(displayData, new LocalTranslation[0], verses);
    view.highlightAyah(new SuraAyah(1, 1), 1, HighlightTypes.AUDIO, false);

    final ViewGroup textScroll = (ViewGroup) view.getChildAt(1);
    final TextView quranTextView = (TextView) textScroll.getChildAt(0);
    final Spanned highlightedText = (Spanned) quranTextView.getText();
    assertThat(highlightedText.getSpans(0, highlightedText.length(), BackgroundColorSpan.class))
        .hasLength(1);

    view.unhighlightAyah(HighlightTypes.AUDIO);
    final Spanned unhighlightedText = (Spanned) quranTextView.getText();
    assertThat(unhighlightedText.getSpans(0, unhighlightedText.length(), BackgroundColorSpan.class))
        .isEmpty();
  }

  @Test
  public void fullPageTafsirModeIncludesArabicRowsComparedToTranslationOnly() {
    final Context context = ApplicationProvider.getApplicationContext();
    final QuranDisplayData displayData = new QuranDisplayData(new QuranInfo(new MadaniDataSource()));
    final LocalTranslation[] translations = new LocalTranslation[] {
        new LocalTranslation(1L, "ar.tafsir.db", "Tafsir", "Translator", "", "", "ar", 1, 2, 1)
    };
    final List<TranslationMetadata> metadata = Collections.singletonList(
        new TranslationMetadata(2, 1, "تفسير الآية", 1, null, null,
            Collections.emptyList(), Collections.emptyList())
    );
    final List<QuranAyahInfo> verses = Collections.singletonList(
        new QuranAyahInfo(2, 1, "الم", metadata, 8)
    );

    final TranslationView fullPageView = new TranslationView(context);
    fullPageView.setDisplayMode(TranslationView.DisplayMode.QURAN_AND_TRANSLATION);
    fullPageView.setVerses(displayData, translations, verses);
    final RecyclerView fullPageRecycler = (RecyclerView) fullPageView.getChildAt(0);

    final TranslationView translationOnlyView = new TranslationView(context);
    translationOnlyView.setDisplayMode(TranslationView.DisplayMode.TRANSLATION_ONLY);
    translationOnlyView.setVerses(displayData, translations, verses);
    final RecyclerView translationOnlyRecycler = (RecyclerView) translationOnlyView.getChildAt(0);

    assertThat(fullPageRecycler.getAdapter().getItemCount())
        .isGreaterThan(translationOnlyRecycler.getAdapter().getItemCount());
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
