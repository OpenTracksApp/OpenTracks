package de.dennisguse.opentracks.ui.util;

import android.view.View;
import android.widget.AbsListView;

import androidx.annotation.NonNull;

/**
 * AbsListView.OnScrollListener class that can be used to know what views in a ListView are currently visible while scrolling.
 */
public class ScrollVisibleViews implements AbsListView.OnScrollListener {
    private int from = -1;
    private int to = -1;

    private final VisibleViewsListener visibleViewsListener;

    public ScrollVisibleViews(@NonNull VisibleViewsListener visibleViewsListener) {
        this.visibleViewsListener = visibleViewsListener;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            if (from >= 0 && to >= 0) {
                for (int i = from; i < to; i++) {
                    View viewChild = view.getChildAt(i - from);
                    visibleViewsListener.onViewVisible(viewChild, i);
                }
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        from = firstVisibleItem;
        to = firstVisibleItem + visibleItemCount;
    }

    public interface VisibleViewsListener {
        void onViewVisible(View view, int position);
    }
}
