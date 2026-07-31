package androidx.viewpager.widget;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
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

  public NonRestoringViewPager(Context context) {
    super(context);
    installReadingScroller(context);
  }

  public NonRestoringViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    installReadingScroller(context);
  }

  public void setIsDualPagesInLandscape(boolean isDualPagesInLandscape) {
    useDefaultImplementation = !isDualPagesInLandscape;
  }

  public void setVerticalPagingEnabled(boolean enabled) {
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

  private static final int VERTICAL_READING_TRANSITION_MS = 650;

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
      // Flowing Quran text owns vertical scrolling. It explicitly requests the
      // adjacent Quran page when its scroll view reaches either boundary.
      return false;
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    try {
      if (verticalPagingEnabled) {
        return false;
      }
      return super.onTouchEvent(ev);
    } catch (IllegalArgumentException e) {
      return false;
    }
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
