package com.quran.labs.androidquran.ui.translation;

import static com.quran.labs.androidquran.ui.PagerActivity.EXTRA_HIGHLIGHT_AYAH;
import static com.quran.labs.androidquran.ui.PagerActivity.EXTRA_HIGHLIGHT_SURA;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.data.model.SuraAyah;
import com.quran.data.model.highlight.HighlightType;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.data.model.selection.SelectionRectangle;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.LocalTranslationDisplaySort;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.common.TranslationMetadata;
import com.quran.labs.androidquran.data.QuranDisplayData;
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils;
import com.quran.labs.androidquran.ui.PagerActivity;
import com.quran.labs.androidquran.ui.helpers.HighlightTypes;
import com.quran.labs.androidquran.ui.helpers.PageNumberSpan;
import com.quran.labs.androidquran.ui.helpers.SuraHeaderSpan;
import com.quran.labs.androidquran.ui.helpers.UthmaniSpan;
import com.quran.labs.androidquran.ui.util.PageController;
import com.quran.labs.androidquran.ui.util.TypefaceManager;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.mobile.translation.model.LocalTranslation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dev.chrisbanes.insetter.Insetter;

public class TranslationView extends FrameLayout implements View.OnClickListener,
    TranslationAdapter.OnVerseSelectedListener, TranslationAdapter.OnJumpToAyahListener {
  private final TranslationAdapter translationAdapter;
  private final RecyclerView translationRecycler;
  private final BoundaryAwareNestedScrollView quranTextScroll;
  private final TextView quranTextView;
  private final TextView emptyTranslationView;
  private final TextView translationPageNumberView;
  private final List<FlowAyahOffset> flowAyahOffsets = new ArrayList<>();
  private final SparseIntArray flowSuraOffsets = new SparseIntArray();

  private SuraAyah selectedAyah;
  private int selectedAyahId = -1;
  private OnClickListener onClickListener;
  private final LinearLayoutManager layoutManager;
  private PageController pageController;
  private LocalTranslation[] localTranslations;
  private SpacerDecoration spacerDecoration;
  private boolean showingQuranText;
  private OnReadingPositionChangedListener readingPositionChangedListener;
  private int lastReportedAyahId = -1;
  private int currentPageNumber = 1;
  private BackgroundColorSpan flowingSelectionSpan;
  private DisplayMode displayMode = DisplayMode.QURAN_TEXT;
  private int pendingScrollSura = -1;
  private int pendingScrollAyah = -1;

  public enum DisplayMode {
    QURAN_TEXT,
    TRANSLATION_ONLY
  }

  public TranslationView(Context context) {
    this(context, null);
  }

  public TranslationView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TranslationView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    translationRecycler = new RecyclerView(context);
    layoutManager = new LinearLayoutManager(context);
    translationRecycler.setLayoutManager(layoutManager);
    translationRecycler.setItemAnimator(new DefaultItemAnimator());
    translationAdapter = new TranslationAdapter(context, translationRecycler, this, this, this);
    translationRecycler.setAdapter(translationAdapter);
    addView(translationRecycler, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    quranTextView = new TextView(context);
    quranTextView.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    quranTextView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
    quranTextView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    quranTextView.setLineSpacing(dp(context, 10), 1.18f);
    quranTextView.setPadding(dp(context, 22), dp(context, 28), dp(context, 22), dp(context, 48));
    quranTextView.setTypeface(TypefaceManager.INSTANCE.getUthmaniTypeface(context));
    quranTextView.setOnClickListener(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      quranTextView.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);
      quranTextView.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
    }

    quranTextScroll = new BoundaryAwareNestedScrollView(context);
    quranTextScroll.setFillViewport(true);
    quranTextScroll.setSmoothScrollingEnabled(true);
    quranTextScroll.addView(quranTextView,
        new NestedScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    quranTextScroll.setVisibility(GONE);
    addView(quranTextScroll, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    emptyTranslationView = new TextView(context);
    emptyTranslationView.setGravity(Gravity.CENTER);
    emptyTranslationView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
    emptyTranslationView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    emptyTranslationView.setText(R.string.translation_not_selected);
    emptyTranslationView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    emptyTranslationView.setTypeface(Typeface.DEFAULT);
    emptyTranslationView.setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 24));
    emptyTranslationView.setVisibility(GONE);
    addView(emptyTranslationView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    translationPageNumberView = new TextView(context);
    translationPageNumberView.setGravity(Gravity.CENTER);
    translationPageNumberView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
    translationPageNumberView.setTypeface(TypefaceManager.INSTANCE.getUthmaniTypeface(context));
    translationPageNumberView.setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 10));
    translationPageNumberView.setVisibility(GONE);
    final FrameLayout.LayoutParams pageNumberLayoutParams = new FrameLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    addView(translationPageNumberView, pageNumberLayoutParams);

    quranTextScroll.setOnScrollChangeListener(
        (NestedScrollView.OnScrollChangeListener) (view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
          notifyAutomaticReadingPosition(scrollY);
          quranTextScroll.checkForPageBoundary(scrollY, oldScrollY);
        });
    quranTextScroll.setBoundaryNavigationListener(new BoundaryNavigationListener() {
      @Override
      public boolean isEnabled() {
        return pageController != null && pageController.isVerticalPageNavigationEnabled();
      }

      @Override
      public void moveToAdjacentPage(boolean forward) {
        if (pageController != null) {
          pageController.requestAdjacentVerticalPage(forward);
        }
      }
    });
    final GestureDetector verseGestureDetector = new GestureDetector(
        context,
        new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onDown(@NonNull MotionEvent event) {
            return true;
          }

          @Override
          public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
            quranTextView.performClick();
            return true;
          }

          @Override
          public void onLongPress(@NonNull MotionEvent event) {
            selectFlowingAyahAt(event);
          }
        });
    quranTextView.setOnTouchListener((view, event) ->
        verseGestureDetector.onTouchEvent(event));

    translationRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
      boolean isDragging = false;

      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        // do not modify the RecyclerView from this method or any method called from
        // the onScrolled listener, since most modification methods cannot be called
        // while the RecyclerView is computing layout or scrolling
        if (selectedAyah != null && isDragging) {
          updateAyahToolBarPosition();
        }
      }

      @Override
      public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        isDragging = newState == RecyclerView.SCROLL_STATE_DRAGGING;
        if (selectedAyah != null && newState == RecyclerView.SCROLL_STATE_IDLE) {
          updateAyahToolBarPosition();
        }
      }
    });

    Insetter.builder()
        .setOnApplyInsetsListener((view, insets, initialState) -> {
          final DisplayCutoutCompat cutout = insets.getDisplayCutout();
          if (cutout != null) {
            final int topSafeOffset = cutout.getSafeInsetTop();
            final int bottomSafeOffset = cutout.getSafeInsetBottom();
            final int horizontalSafeOffset =
                Math.max(cutout.getSafeInsetLeft(), cutout.getSafeInsetRight());

            if (spacerDecoration == null) {
              spacerDecoration = new SpacerDecoration(topSafeOffset, bottomSafeOffset);
              translationRecycler.addItemDecoration(spacerDecoration);
            } else {
              if (spacerDecoration.setOffsets(topSafeOffset, bottomSafeOffset)) {
                translationRecycler.invalidateItemDecorations();
              }
            }

            setPadding(horizontalSafeOffset, 0, horizontalSafeOffset, 0);
          }
        })
        .applyToView(this);
  }

  public void setPageController(PageController controller) {
    this.pageController = controller;
  }

  public void setVerses(@NonNull QuranDisplayData quranDisplayData,
                        @NonNull LocalTranslation[] translations,
                        @NonNull List<QuranAyahInfo> verses) {
    localTranslations = translations;
    if (displayMode == DisplayMode.QURAN_TEXT) {
      emptyTranslationView.setVisibility(GONE);
      showFlowingQuranText(quranDisplayData, verses);
      return;
    }

    showingQuranText = false;
    quranTextScroll.setVisibility(GONE);
    if (translations.length == 0) {
      translationRecycler.setVisibility(GONE);
      emptyTranslationView.setVisibility(VISIBLE);
      flowAyahOffsets.clear();
      return;
    }

    emptyTranslationView.setVisibility(GONE);
    translationRecycler.setVisibility(VISIBLE);

    List<TranslationViewRow> rows = new ArrayList<>();
    int currentSura = -1;
    boolean wantTranslationHeaders = translations.length > 1;
    for (int i = 0, size = verses.size(); i < size; i++) {
      QuranAyahInfo verse = verses.get(i);
      int sura = verse.sura;
      if (sura != currentSura) {
        rows.add(new TranslationViewRow(TranslationViewRow.Type.SURA_HEADER, verse,
            quranDisplayData.getSuraName(getContext(), sura, true)));
        currentSura = sura;
      }

      rows.add(new TranslationViewRow(TranslationViewRow.Type.VERSE_NUMBER, verse));

      final LocalTranslation[] sortedTranslations = Arrays.copyOf(translations, translations.length);
      Arrays.sort(sortedTranslations, new LocalTranslationDisplaySort());

      for (int j = 0; j < sortedTranslations.length; j++) {
        final TranslationMetadata metadata = findText(verse.texts, (int) sortedTranslations[j].getId());
        CharSequence text = metadata != null ? metadata.getText() : "";
        if (!TextUtils.isEmpty(text)) {
          if (wantTranslationHeaders) {
            rows.add(
                new TranslationViewRow(TranslationViewRow.Type.TRANSLATOR,
                    verse,
                    sortedTranslations[j].resolveTranslatorName()));
          }
          rows.add(new TranslationViewRow(
              TranslationViewRow.Type.TRANSLATION_TEXT, verse, text, j,
              metadata == null ? null : metadata.getLink(),
              metadata == null ? null : metadata.getLinkPageNumber(),
              "ar".equals(sortedTranslations[j].getLanguageCode()),
              metadata == null ? Collections.emptyList() : metadata.getAyat(),
              metadata == null ? Collections.emptyList() : metadata.getFootnotes()
              ));
        }
      }

      rows.add(new TranslationViewRow(TranslationViewRow.Type.SPACER, verse));
    }

    translationAdapter.setData(rows);
    translationAdapter.notifyDataSetChanged();
  }

  public void refresh(@NonNull QuranSettings quranSettings) {
    translationAdapter.refresh(quranSettings);
    applyQuranTextSize(quranSettings.getAyahTextSize());
    if (quranSettings.isNightMode()) {
      final int brightness = quranSettings.getNightModeTextBrightness();
      quranTextView.setTextColor(Color.rgb(brightness, brightness, brightness));
      emptyTranslationView.setTextColor(Color.rgb(brightness, brightness, brightness));
      translationPageNumberView.setTextColor(Color.rgb(brightness, brightness, brightness));
    } else {
      quranTextView.setTextColor(Color.rgb(22, 40, 72));
      emptyTranslationView.setTextColor(Color.rgb(22, 40, 72));
      translationPageNumberView.setTextColor(Color.rgb(22, 40, 72));
    }
  }

  public void setDisplayMode(@NonNull DisplayMode displayMode) {
    this.displayMode = displayMode;
    translationPageNumberView.setVisibility(
        displayMode == DisplayMode.TRANSLATION_ONLY ? VISIBLE : GONE);
  }

  private void showFlowingQuranText(@NonNull QuranDisplayData quranDisplayData,
                                    @NonNull List<QuranAyahInfo> verses) {
    showingQuranText = true;
    translationRecycler.setVisibility(GONE);
    quranTextScroll.setVisibility(VISIBLE);
    flowAyahOffsets.clear();
    flowSuraOffsets.clear();
    lastReportedAyahId = -1;

    final SpannableStringBuilder text = new SpannableStringBuilder();
    int currentSura = -1;
    for (QuranAyahInfo verse : verses) {
      if (verse.sura != currentSura) {
        final boolean isFirstHeaderOnPage = text.length() == 0;
        if (isFirstHeaderOnPage) {
          // Keep the page title away from the very top without moving the Quran text lower.
          text.append("\n");
        } else {
          // Move an internal sura title down while preserving the total space around it.
          text.append("\n\n\n");
        }
        final int headerStart = text.length();
        flowSuraOffsets.put(verse.sura, headerStart);
        text.append(quranDisplayData.getSuraName(getContext(), verse.sura, true));
        text.setSpan(new SuraHeaderSpan(getContext()), headerStart, text.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append("\n");
        if (verse.ayah == 1 && verse.sura != 1 && verse.sura != 9) {
          text.append(ArabicDatabaseUtils.AR_BASMALLAH).append("\n\n");
        }
        currentSura = verse.sura;
      }

      if (verse.arabicText != null) {
        final int ayahStart = text.length();
        text.append(ArabicDatabaseUtils.Companion.getAyahWithoutBasmallah(
            verse.sura, verse.ayah, verse.arabicText));
        text.append(" ﴿")
            .append(QuranUtils.getLocalizedNumber(verse.ayah))
            .append("﴾  ");
        flowAyahOffsets.add(new FlowAyahOffset(ayahStart, text.length(), verse));
      }
    }

    if (text.length() > 0) {
      text.append("\n\n");
      final int pageNumberStart = text.length();
      text.append(QuranUtils.getLocalizedNumber(currentPageNumber));
      text.setSpan(new PageNumberSpan(getContext()), pageNumberStart, text.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    if (text.length() > 0) {
      text.setSpan(new UthmaniSpan(getContext()), 0, text.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    quranTextView.setText(text, TextView.BufferType.SPANNABLE);
    refresh(QuranSettings.getInstance(getContext()));
    if (selectedAyah != null) {
      scrollFlowingTextToAyah(selectedAyah.sura, selectedAyah.ayah);
    }
    quranTextScroll.post(() -> notifyAutomaticReadingPosition(quranTextScroll.getScrollY()));
  }

  public void setTranslationClickedListener(OnClickListener listener) {
    onClickListener = listener;
  }

  public void setReadingPositionChangedListener(OnReadingPositionChangedListener listener) {
    readingPositionChangedListener = listener;
  }

  public void setPageNumber(int pageNumber) {
    currentPageNumber = pageNumber;
    translationPageNumberView.setText(QuranUtils.getLocalizedNumber(pageNumber));
  }

  private void applyQuranTextSize(float size) {
    quranTextView.setTextSize(
        TypedValue.COMPLEX_UNIT_SP,
        TranslationAdapter.ARABIC_MULTIPLIER * size);
  }

  @Override
  public void onJumpToAyah(@NonNull SuraAyah target, int page) {
    final Context context = getContext();
    Intent i = new Intent(getContext(), PagerActivity.class);
    i.putExtra("page", page);
    i.putExtra(EXTRA_HIGHLIGHT_SURA, target.sura);
    i.putExtra(EXTRA_HIGHLIGHT_AYAH, target.ayah);
    context.startActivity(i);
  }

  public void highlightAyah(SuraAyah suraAyah, int ayahId, HighlightType highlightType) {
    if (highlightType.equals(HighlightTypes.SELECTION)) {
      selectedAyah = suraAyah;
      selectedAyahId = ayahId;
    } else if (selectedAyah != null) {
      hideMenu();
    }

    if (showingQuranText && shouldHandleHighlightType(highlightType)) {
      if (highlightType.equals(HighlightTypes.SELECTION)) {
        highlightFlowingAyah(suraAyah.sura, suraAyah.ayah);
      }
      scrollFlowingTextToAyah(suraAyah.sura, suraAyah.ayah);
    } else if (shouldHandleHighlightType(highlightType)) {
      translationAdapter.setHighlightedAyah(ayahId, highlightType);
    }
  }

  private boolean shouldHandleHighlightType(HighlightType highlightType) {
    return highlightType.equals(HighlightTypes.AUDIO) || highlightType.equals(HighlightTypes.SELECTION);
  }

  private void hideMenu() {
    pageController.endAyahMode();
  }

  public void unhighlightAyah(HighlightType highlightType) {
    if (highlightType.equals(HighlightTypes.SELECTION)) {
      selectedAyah = null;
      selectedAyahId = -1;
    }

    if (shouldHandleHighlightType(highlightType)) {
      if (highlightType.equals(HighlightTypes.SELECTION)) {
        clearFlowingSelection();
      }
      translationAdapter.unhighlight();
      if (selectedAyah != null) {
        // imples that it's not selection, so let's reselect the selected ayah
        translationAdapter.setHighlightedAyah(selectedAyahId, HighlightTypes.SELECTION);
      }
    }
  }

  public void unhighlightAyat(HighlightType highlightType) {
    if (selectedAyah != null && highlightType.equals(HighlightTypes.SELECTION)) {
      selectedAyah = null;
      selectedAyahId = -1;
    }

    if (shouldHandleHighlightType(highlightType)) {
      if (highlightType.equals(HighlightTypes.SELECTION)) {
        clearFlowingSelection();
      }
      translationAdapter.unhighlight();
      if (selectedAyah != null) {
        // imples that it's not selection, so let's reselect the selected ayah
        translationAdapter.setHighlightedAyah(selectedAyahId, HighlightTypes.SELECTION);
      }
    }
  }

  @Override
  public void onClick(View v) {
    if (selectedAyah != null) {
      hideMenu();
      selectedAyah = null;
      selectedAyahId = -1;
      return;
    }

    if (onClickListener != null) {
      onClickListener.onClick(v);
    }
  }

  private TranslationMetadata findText(List<TranslationMetadata> texts, Integer translationId) {
    for (TranslationMetadata text : texts) {
      if (translationId.equals(text.getLocalTranslationId())) {
        return text;
      }
    }
    return null;
  }

  @Nullable
  public QuranAyahInfo getQuranAyahInfo(int sura, int ayah) {
    if (selectedAyah != null && selectedAyah.sura == sura && selectedAyah.ayah == ayah) {
      if (showingQuranText) {
        for (FlowAyahOffset offset : flowAyahOffsets) {
          if (offset.ayahInfo.sura == sura && offset.ayahInfo.ayah == ayah) {
            return offset.ayahInfo;
          }
        }
      } else {
        return translationAdapter.highlightedAyahInfo();
      }
    } else {
      return null;
    }
    return null;
  }

  @Nullable
  public LocalTranslation[] getLocalTranslations() {
    return localTranslations;
  }

  public SelectionIndicator getToolbarPosition(int sura, int ayah) {
    if (showingQuranText) {
      return getFlowingTextToolbarPosition(sura, ayah);
    }
    int[] versePopupPosition = translationAdapter.getSelectedVersePopupPosition(sura, ayah);
    if (versePopupPosition != null) {
      return getToolbarPosition(versePopupPosition);
    }
    return SelectionIndicator.None.INSTANCE;
  }

  public SelectionIndicator getToolbarPosition() {
    if (showingQuranText && selectedAyah != null) {
      return getFlowingTextToolbarPosition(selectedAyah.sura, selectedAyah.ayah);
    }
    int[] versePopupPosition = translationAdapter.getSelectedVersePopupPosition();
    if (versePopupPosition != null) {
      return getToolbarPosition(versePopupPosition);
    }
    return SelectionIndicator.None.INSTANCE;
  }

  private SelectionIndicator getToolbarPosition(int[] versePopupPosition) {
    // for dual screen tablet mode, we need to add the view's x (so clicks on the
    // right page properly show on the right page and not on the left one).
    final int[] positionOnScreen = new int[2];
    getLocationOnScreen(positionOnScreen);
    final int xOffset = positionOnScreen[0];

    return new SelectionIndicator.SelectedPointPosition(
        xOffset + versePopupPosition[0],
        versePopupPosition[1],
        0f,
        0f
    );
  }

  private SelectionIndicator getFlowingTextToolbarPosition(int sura, int ayah) {
    final Layout layout = quranTextView.getLayout();
    if (layout == null) {
      return SelectionIndicator.None.INSTANCE;
    }
    for (FlowAyahOffset offset : flowAyahOffsets) {
      if (offset.ayahInfo.sura == sura && offset.ayahInfo.ayah == ayah) {
        final int firstLine = layout.getLineForOffset(offset.characterOffset);
        final int lastLine = layout.getLineForOffset(
            Math.max(offset.characterOffset, offset.endCharacterOffset - 1));
        final int[] positionOnScreen = new int[2];
        getLocationOnScreen(positionOnScreen);
        final float xOffset = positionOnScreen[0] + quranTextView.getTotalPaddingLeft();
        final float yOffset = quranTextView.getTotalPaddingTop() - quranTextScroll.getScrollY();
        final SelectionRectangle firstBounds = new SelectionRectangle(
            xOffset + layout.getLineLeft(firstLine),
            yOffset + layout.getLineTop(firstLine),
            xOffset + layout.getLineRight(firstLine),
            yOffset + layout.getLineBottom(firstLine)
        );
        final SelectionRectangle lastBounds = new SelectionRectangle(
            xOffset + layout.getLineLeft(lastLine),
            yOffset + layout.getLineTop(lastLine),
            xOffset + layout.getLineRight(lastLine),
            yOffset + layout.getLineBottom(lastLine)
        );
        return new SelectionIndicator.SelectedItemPosition(
            firstBounds,
            lastBounds,
            0f,
            0f
        );
      }
    }
    return SelectionIndicator.None.INSTANCE;
  }

  /**
   * This method updates the toolbar position when an ayah is selected
   * This method is called from the onScroll listener, and as thus must make sure not to ask
   * the RecyclerView to change anything (otherwise, it will result in a crash, as methods to
   * update the RecyclerView cannot be called amidst scrolling or computing of a layout).
   */
  private void updateAyahToolBarPosition() {
    final SelectionIndicator position = getToolbarPosition();
    if (position instanceof SelectionIndicator.SelectedPointPosition) {
      final SelectionIndicator.SelectedPointPosition selectedPointPosition =
          (SelectionIndicator.SelectedPointPosition) position;
      if (selectedPointPosition.getY() > getHeight() || selectedPointPosition.getY() < 0) {
        hideMenu();
      } else {
        pageController.onScrollChanged(0);
      }
    }
  }

  @Override
  public void onVerseSelected(@NonNull QuranAyahInfo ayahInfo) {
    final SuraAyah suraAyah = new SuraAyah(ayahInfo.sura, ayahInfo.ayah);
    if (selectedAyah != null) {
      final boolean isUnselectingSelectedVerse = selectedAyah.equals(suraAyah);
      hideMenu();
      if (isUnselectingSelectedVerse) {
        return;
      }
    } else {
      // hide the menu because the previous page might have had
      // something selected here (which would break selection).
      hideMenu();
    }

    pageController.handleLongPress(suraAyah);
  }

  public int findFirstCompletelyVisibleItemPosition() {
    if (showingQuranText) {
      return quranTextScroll.getScrollY();
    }
    return layoutManager.findFirstCompletelyVisibleItemPosition();
  }

  public void setScrollPosition(int position) {
    if (showingQuranText) {
      // A bookmark jump has priority over a fragment's previously restored
      // page offset. Otherwise the restored zero offset hides the target ayah.
      if (pendingScrollSura > 0 && pendingScrollAyah > 0) {
        scrollFlowingTextToAyah(pendingScrollSura, pendingScrollAyah);
        return;
      }
      quranTextScroll.post(() -> quranTextScroll.scrollTo(0, Math.max(0, position)));
    } else {
      layoutManager.scrollToPosition(position);
    }
  }

  public void scrollToPageStart() {
    quranTextScroll.post(() -> quranTextScroll.scrollTo(0, 0));
  }

  public void scrollToPageEnd() {
    quranTextScroll.post(() ->
        quranTextScroll.scrollTo(0, Math.max(0,
            quranTextView.getHeight() - quranTextScroll.getHeight())));
  }

  public boolean isFlowingQuranTextReady() {
    return showingQuranText && quranTextView.getText().length() > 0 &&
        quranTextView.getLayout() != null;
  }

  private void notifyAutomaticReadingPosition(int scrollY) {
    if (!showingQuranText || readingPositionChangedListener == null ||
        flowAyahOffsets.isEmpty() || quranTextView.getLayout() == null) {
      return;
    }

    final int readingLine = quranTextView.getLayout().getLineForVertical(
        Math.max(0, scrollY + dp(getContext(), 24)));
    final int characterOffset = quranTextView.getLayout().getLineStart(readingLine);
    FlowAyahOffset visibleAyah = flowAyahOffsets.get(0);
    for (FlowAyahOffset candidate : flowAyahOffsets) {
      if (candidate.characterOffset > characterOffset) {
        break;
      }
      visibleAyah = candidate;
    }
    if (visibleAyah.ayahInfo.ayahId != lastReportedAyahId) {
      lastReportedAyahId = visibleAyah.ayahInfo.ayahId;
      readingPositionChangedListener.onReadingPositionChanged(visibleAyah.ayahInfo);
    }
  }

  private void scrollFlowingTextToAyah(int sura, int ayah) {
    pendingScrollSura = sura;
    pendingScrollAyah = ayah;
    for (FlowAyahOffset offset : flowAyahOffsets) {
      if (offset.ayahInfo.sura == sura && offset.ayahInfo.ayah == ayah) {
        final int targetOffset =
            ayah == 1 ? flowSuraOffsets.get(sura, offset.characterOffset) : offset.characterOffset;
        postScrollToAyah(targetOffset, 0);
        return;
      }
    }
  }

  private void postScrollToAyah(int targetOffset, int attempt) {
    quranTextView.post(() -> {
      if (quranTextView.getLayout() == null) {
        if (attempt < 12) {
          postScrollToAyah(targetOffset, attempt + 1);
        }
        return;
      }
      final int line = quranTextView.getLayout().getLineForOffset(targetOffset);
      final int top = Math.max(0,
          quranTextView.getLayout().getLineTop(line) - dp(getContext(), 20));
      quranTextScroll.smoothScrollTo(0, top);
      final int expectedSura = pendingScrollSura;
      final int expectedAyah = pendingScrollAyah;
      quranTextView.postDelayed(() -> {
        if (pendingScrollSura == expectedSura && pendingScrollAyah == expectedAyah) {
          pendingScrollSura = -1;
          pendingScrollAyah = -1;
        }
      }, 1500L);
    });
  }

  private void selectFlowingAyahAt(@NonNull MotionEvent event) {
    if (!showingQuranText || quranTextView.getLayout() == null) {
      return;
    }
    final Layout layout = quranTextView.getLayout();
    final int x = Math.max(0, Math.round(event.getX()) - quranTextView.getTotalPaddingLeft());
    final int y = Math.max(0, Math.round(event.getY()) - quranTextView.getTotalPaddingTop());
    final int line = layout.getLineForVertical(y);
    final int characterOffset = layout.getOffsetForHorizontal(line, x);
    for (FlowAyahOffset candidate : flowAyahOffsets) {
      if (characterOffset >= candidate.characterOffset &&
          characterOffset < candidate.endCharacterOffset) {
        onVerseSelected(candidate.ayahInfo);
        return;
      }
    }
  }

  private void highlightFlowingAyah(int sura, int ayah) {
    clearFlowingSelection();
    final CharSequence value = quranTextView.getText();
    if (!(value instanceof Spannable)) {
      return;
    }
    for (FlowAyahOffset offset : flowAyahOffsets) {
      if (offset.ayahInfo.sura == sura && offset.ayahInfo.ayah == ayah) {
        flowingSelectionSpan = new BackgroundColorSpan(0x554CAF50);
        ((Spannable) value).setSpan(
            flowingSelectionSpan,
            offset.characterOffset,
            offset.endCharacterOffset,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return;
      }
    }
  }

  private void clearFlowingSelection() {
    final CharSequence value = quranTextView.getText();
    if (flowingSelectionSpan != null && value instanceof Spannable) {
      ((Spannable) value).removeSpan(flowingSelectionSpan);
    }
    flowingSelectionSpan = null;
  }

  private static int dp(Context context, int value) {
    return Math.round(TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
  }

  public interface OnReadingPositionChangedListener {
    void onReadingPositionChanged(@NonNull QuranAyahInfo ayahInfo);
  }

  private interface BoundaryNavigationListener {
    boolean isEnabled();
    void moveToAdjacentPage(boolean forward);
  }

  private static final class BoundaryAwareNestedScrollView extends NestedScrollView {
    private static final long USER_SCROLL_WINDOW_MS = 1000L;

    private BoundaryNavigationListener boundaryNavigationListener;
    private long lastUserTouchAt;
    private float lastTouchY;
    private float gestureDistance;
    private final int minimumGestureDistance;
    private int lastUserDirection;
    private boolean boundaryRequestPosted;

    BoundaryAwareNestedScrollView(Context context) {
      super(context);
      minimumGestureDistance = ViewConfiguration.get(context).getScaledTouchSlop() * 2;
    }

    void setBoundaryNavigationListener(BoundaryNavigationListener listener) {
      boundaryNavigationListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
      final int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN) {
        lastTouchY = event.getY();
        gestureDistance = 0f;
        lastUserDirection = 0;
        boundaryRequestPosted = false;
        lastUserTouchAt = SystemClock.uptimeMillis();
      } else if (action == MotionEvent.ACTION_MOVE) {
        final float currentY = event.getY();
        final float delta = currentY - lastTouchY;
        if (Math.abs(delta) >= 1f) {
          // Finger moving upward means reading forward.
          lastUserDirection = delta < 0f ? 1 : -1;
          gestureDistance += Math.abs(delta);
          lastTouchY = currentY;
        }
        lastUserTouchAt = SystemClock.uptimeMillis();
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        lastUserTouchAt = SystemClock.uptimeMillis();
      }
      final boolean handled = super.dispatchTouchEvent(event);
      if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
        checkForPageBoundary(getScrollY(), getScrollY());
      }
      return handled;
    }

    void checkForPageBoundary(int scrollY, int oldScrollY) {
      if (boundaryRequestPosted || boundaryNavigationListener == null ||
          !boundaryNavigationListener.isEnabled() ||
          gestureDistance < minimumGestureDistance ||
          SystemClock.uptimeMillis() - lastUserTouchAt > USER_SCROLL_WINDOW_MS) {
        return;
      }

      final boolean movingForward = scrollY > oldScrollY || lastUserDirection > 0;
      final boolean movingBackward = scrollY < oldScrollY || lastUserDirection < 0;
      if (movingForward && !canScrollVertically(1)) {
        postBoundaryRequest(true);
      } else if (movingBackward && !canScrollVertically(-1)) {
        postBoundaryRequest(false);
      }
    }

    private void postBoundaryRequest(boolean forward) {
      boundaryRequestPosted = true;
      post(() -> {
        if (boundaryNavigationListener != null && boundaryNavigationListener.isEnabled()) {
          boundaryNavigationListener.moveToAdjacentPage(forward);
        }
      });
    }
  }

  private static final class FlowAyahOffset {
    final int characterOffset;
    final int endCharacterOffset;
    final QuranAyahInfo ayahInfo;

    FlowAyahOffset(int characterOffset, int endCharacterOffset, QuranAyahInfo ayahInfo) {
      this.characterOffset = characterOffset;
      this.endCharacterOffset = endCharacterOffset;
      this.ayahInfo = ayahInfo;
    }
  }

}
