package androidx.viewpager.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.lang.reflect.Field;

/**
 * NonRestoringViewPager is a hack to sometimes prevent ViewPager from restoring its
 * page in onRestoreInstanceState. This is done because in some cases, the ViewPager
 * has a different number of pages in portrait and landscape, and so restoring the
 * old page number is incorrect (and requires making 3 fragments that will be thrown
 * away shortly thereafter).
 *
 * It also contains the same swallowing of an Exception that is occasionally thrown
 * on certain devices in onTouchEvent (this hack is also present in QuranViewPager).
 *
 * Note that the package name for this is a hack to allow overriding setCurrentItemInternal,
 * which is package protected, so as to be able to ignore the initial "restore page" event
 * without ignoring the entirety of saving and restoring of state.
 *
 * Hopefully, this is a short term solution. Note that an alternative solution that also
 * works is to handle our own SavedState here (and call super during save, but ignore the
 * result, and call super during restore with null). This, however, seemed a bit cleaner
 * for now, since we still want to get save and restore on the PagerAdapter.
 */
public class NonRestoringViewPager extends ViewPager {
  private boolean isRestoring = false;
  private boolean useDefaultImplementation;
  private boolean verticalPagingEnabled;
  private boolean childOwnsVerticalScroll;
  private float verticalGestureStartY = Float.NaN;
  private int verticalGestureStartItem = -1;
  private int verticalGesturePointerId = MotionEvent.INVALID_POINTER_ID;
  private boolean verticalTransitionInProgress;
  private boolean queueVerticalGestureUntilSettled;
  private int queuedVerticalPageDelta;

  public NonRestoringViewPager(Context context) {
    super(context);
    installReadingScroller(context);
    installVerticalPagingQueue();
  }

  public NonRestoringViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    installReadingScroller(context);
    installVerticalPagingQueue();
  }

  public void setIsDualPagesInLandscape(boolean isDualPagesInLandscape) {
    useDefaultImplementation = !isDualPagesInLandscape;
  }

  public void setVerticalPagingEnabled(boolean enabled) {
    setVerticalPagingEnabled(enabled, false);
  }

  public void setVerticalPagingEnabled(boolean enabled, boolean childOwnsVerticalScroll) {
    this.childOwnsVerticalScroll = childOwnsVerticalScroll;
    if (!enabled || childOwnsVerticalScroll) {
      queuedVerticalPageDelta = 0;
      queueVerticalGestureUntilSettled = false;
      clearVerticalGesture();
    }
    if (verticalPagingEnabled == enabled) {
      return;
    }

    verticalPagingEnabled = enabled;
    setPageTransformer(enabled, enabled ? new VerticalPageTransformer() : null);
    if (!enabled) {
      for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        child.setAlpha(1f);
        child.setTranslationX(0f);
        child.setTranslationY(0f);
      }
    }
    requestLayout();
  }

  private void installReadingScroller(Context context) {
    try {
      final Field scrollerField = ViewPager.class.getDeclaredField("mScroller");
      scrollerField.setAccessible(true);
      scrollerField.set(this, new Scroller(context, new DecelerateInterpolator()) {
        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
          super.startScroll(
              startX,
              startY,
              dx,
              dy,
              verticalPagingEnabled ? VERTICAL_READING_TRANSITION_MS : duration);
        }
      });
    } catch (ReflectiveOperationException ignored) {
      // Keep the platform ViewPager duration if its internal implementation changes.
    }
  }

  static final int VERTICAL_READING_TRANSITION_MS = 120;

  @Override
  void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
    if (useDefaultImplementation || !isRestoring) {
      super.setCurrentItemInternal(item, smoothScroll, always);
    }
  }

  @Override
  void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
    if (useDefaultImplementation || !isRestoring) {
      super.setCurrentItemInternal(item, smoothScroll, always, velocity);
    }
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    isRestoring = true;
    super.onRestoreInstanceState(state);
    isRestoring = false;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (verticalPagingEnabled) {
      if (childOwnsVerticalScroll) {
        // Flowing Quran text owns vertical scrolling and explicitly requests the
        // adjacent Quran page when its scroll view reaches either boundary.
        return false;
      }
      rememberVerticalGestureStart(ev);
      if (queueVerticalGestureUntilSettled) {
        return shouldInterceptQueuedVerticalGesture(ev);
      }
      final MotionEvent transformedEvent = transformTouchEventForVerticalReading(
          ev, getWidth(), getHeight());
      try {
        return super.onInterceptTouchEvent(transformedEvent);
      } finally {
        transformedEvent.recycle();
      }
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    try {
      if (verticalPagingEnabled) {
        if (childOwnsVerticalScroll) {
          return false;
        }
        rememberVerticalGestureStart(ev);
        if (queueVerticalGestureUntilSettled) {
          handleQueuedVerticalGesture(ev);
          return true;
        }
        final MotionEvent transformedEvent = transformTouchEventForVerticalReading(
            ev, getWidth(), getHeight());
        try {
          final boolean handled = super.onTouchEvent(transformedEvent);
          recoverDroppedVerticalGesture(ev);
          return handled;
        } finally {
          transformedEvent.recycle();
        }
      }
      return super.onTouchEvent(ev);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private void rememberVerticalGestureStart(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      verticalGestureStartY = event.getY();
      verticalGestureStartItem = getCurrentItem();
      verticalGesturePointerId = event.getPointerId(0);
      queueVerticalGestureUntilSettled = verticalTransitionInProgress;
    } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      clearVerticalGesture();
    }
  }

  private boolean shouldInterceptQueuedVerticalGesture(MotionEvent event) {
    final int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
      clearVerticalGesture();
      return false;
    }
    if (action != MotionEvent.ACTION_MOVE || Float.isNaN(verticalGestureStartY)) {
      return false;
    }

    final int pointerIndex = event.findPointerIndex(verticalGesturePointerId);
    if (pointerIndex < 0) {
      clearVerticalGesture();
      return false;
    }
    return Math.abs(event.getY(pointerIndex) - verticalGestureStartY)
        >= minimumRecoverySwipeDistance();
  }

  private void handleQueuedVerticalGesture(MotionEvent event) {
    final int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_CANCEL) {
      clearVerticalGesture();
      return;
    }
    if (action != MotionEvent.ACTION_UP || Float.isNaN(verticalGestureStartY)) {
      return;
    }

    final int pointerIndex = event.findPointerIndex(verticalGesturePointerId);
    if (pointerIndex >= 0) {
      final int adapterCount = getAdapter() == null ? 0 : getAdapter().getCount();
      final int direction = verticalGestureDirection(
          verticalGestureStartY,
          event.getY(pointerIndex),
          minimumRecoverySwipeDistance());
      queuedVerticalPageDelta = accumulateVerticalPageDelta(
          getCurrentItem(), queuedVerticalPageDelta, direction, adapterCount);
    }
    clearVerticalGesture();
    if (!verticalTransitionInProgress) {
      drainQueuedVerticalPages();
    }
  }

  private void installVerticalPagingQueue() {
    addOnPageChangeListener(new SimpleOnPageChangeListener() {
      @Override
      public void onPageScrollStateChanged(int state) {
        verticalTransitionInProgress = state != SCROLL_STATE_IDLE;
        if (!verticalTransitionInProgress) {
          drainQueuedVerticalPages();
        }
      }
    });
  }

  private void recoverDroppedVerticalGesture(MotionEvent event) {
    if (event.getActionMasked() != MotionEvent.ACTION_UP || Float.isNaN(verticalGestureStartY)) {
      return;
    }

    final int pointerIndex = event.findPointerIndex(verticalGesturePointerId);
    if (pointerIndex < 0) {
      clearVerticalGesture();
      return;
    }

    final int adapterCount = getAdapter() == null ? 0 : getAdapter().getCount();
    final int target = verticalGestureTarget(
        verticalGestureStartItem,
        verticalGestureStartY,
        event.getY(pointerIndex),
        minimumRecoverySwipeDistance(),
        adapterCount);
    if (target >= 0 && getCurrentItem() == verticalGestureStartItem && target != getCurrentItem()) {
      setCurrentItem(target, true);
    }
    clearVerticalGesture();
  }

  private void clearVerticalGesture() {
    verticalGestureStartY = Float.NaN;
    verticalGestureStartItem = -1;
    verticalGesturePointerId = MotionEvent.INVALID_POINTER_ID;
    queueVerticalGestureUntilSettled = false;
  }

  private float minimumRecoverySwipeDistance() {
    return Math.max(
        ViewConfiguration.get(getContext()).getScaledPagingTouchSlop() * 2f,
        getHeight() * MINIMUM_RECOVERY_SWIPE_RATIO);
  }

  private void drainQueuedVerticalPages() {
    if (!verticalPagingEnabled || childOwnsVerticalScroll || queuedVerticalPageDelta == 0) {
      return;
    }

    final int adapterCount = getAdapter() == null ? 0 : getAdapter().getCount();
    final int currentItem = getCurrentItem();
    final int target = clampVerticalPageTarget(
        currentItem + queuedVerticalPageDelta, adapterCount);
    queuedVerticalPageDelta = 0;
    if (target >= 0 && target != currentItem) {
      setCurrentItem(target, true);
    }
  }

  static int verticalGestureTarget(
      int startItem,
      float startY,
      float endY,
      float minimumDistance,
      int itemCount) {
    if (startItem < 0 || itemCount <= 0) {
      return -1;
    }

    // Upward reading advances the page. In this RTL adapter advancing decreases
    // the ViewPager position, while a downward gesture returns to the previous page.
    final int direction = verticalGestureDirection(startY, endY, minimumDistance);
    if (direction == 0) {
      return -1;
    }
    final int target = startItem + direction;
    return target >= 0 && target < itemCount ? target : -1;
  }

  static int verticalGestureDirection(float startY, float endY, float minimumDistance) {
    if (Math.abs(endY - startY) < minimumDistance) {
      return 0;
    }
    return endY < startY ? -1 : 1;
  }

  static int accumulateVerticalPageDelta(
      int currentItem, int pendingDelta, int gestureDirection, int itemCount) {
    if (gestureDirection == 0 || currentItem < 0 || itemCount <= 0) {
      return pendingDelta;
    }
    final int target = clampVerticalPageTarget(
        currentItem + pendingDelta + gestureDirection, itemCount);
    return target < 0 ? pendingDelta : target - currentItem;
  }

  private static int clampVerticalPageTarget(int target, int itemCount) {
    if (itemCount <= 0) {
      return -1;
    }
    return Math.max(0, Math.min(target, itemCount - 1));
  }

  private static final float MINIMUM_RECOVERY_SWIPE_RATIO = 0.12f;

  static MotionEvent transformTouchEventForVerticalReading(
      MotionEvent event, float width, float height) {
    final MotionEvent transformedEvent = MotionEvent.obtain(event);
    if (width > 0f && height > 0f) {
      final Matrix matrix = new Matrix();
      matrix.setValues(verticalReadingTransformValues(width, height));
      transformedEvent.transform(matrix);
    }
    return transformedEvent;
  }

  static float[] verticalReadingTransformValues(float width, float height) {
    return new float[] {
        0f, -width / height, width,
        height / width, 0f, 0f,
        0f, 0f, 1f
    };
  }

  // Quran pages advance with a rightward horizontal gesture in the RTL pager.
  // Mirroring the vertical axis maps the conventional upward reading gesture
  // to that same forward direction.
  static float verticalToHorizontalX(float y, float width, float height) {
    return (height - y) / height * width;
  }

  static float verticalToHorizontalY(float x, float width, float height) {
    return x / width * height;
  }

  private static class VerticalPageTransformer implements PageTransformer {
    @Override
    public void transformPage(View page, float position) {
      if (position < -1f || position > 1f) {
        page.setAlpha(0f);
      } else {
        page.setAlpha(1f);
        page.setTranslationX(page.getWidth() * -position);
        page.setTranslationY(page.getHeight() * -position);
      }
    }
  }
}
