package com.fuzz.android.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.fuzz.android.R;

/**
 * Container for the articles view, which also manages horizontal swiping to reveal the categories.
 */
public class ArticlesContainerView extends FrameLayout {
    public static final byte DIRECTION_VERTICAL = 2;
    public static final byte DIRECTION_HORIZONTAL = 1;
    public static final byte NONE = 0;
    private CategoriesView categoriesView;
    private float oldTouchX;
    private float oldTouchY;
    private float initialTouchX;
    private float initialTouchY;
    private int scrollThreshold;
    private int categoriesSwipeDistance;
    private byte scrollingDirection = NONE;
    private ArticlesView articlesView;
    private android.os.Handler handler;
    private boolean wasCategoriesVisible;

    public ArticlesContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        //  Cache resources
        Resources res = context.getResources();
        scrollThreshold = res.getDimensionPixelSize(R.dimen.scroll_threshold);
        categoriesSwipeDistance = res.getDimensionPixelSize(R.dimen.categories_swipe_distance);

        handler = new Handler();
    }

    public ArticlesView getArticlesView() {
        return articlesView;
    }

    public void setArticlesView(ArticlesView articlesView) {
        this.articlesView = articlesView;
    }

    public CategoriesView getCategoriesView() {
        return categoriesView;
    }

    public void setCategoriesView(CategoriesView categoriesView) {
        this.categoriesView = categoriesView;
    }

    public byte getScrollingDirection(){
        return scrollingDirection;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        int action = e.getAction();

        float x = e.getRawX();
        float y = e.getRawY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = x;
                initialTouchY = y;

                wasCategoriesVisible = categoriesView.isVisible();
                articlesView.setScrollable(false);
                break;

            case MotionEvent.ACTION_MOVE:

                if (articlesView.isDraggingArticle()) {
                    break;
                }

                float deltaXInitial = x - initialTouchX;
                float deltaYInitial = y - initialTouchY;
                if (scrollingDirection == NONE) {
                    if ((categoriesView.isVisible() ? Math.abs(deltaXInitial) : deltaXInitial) >= scrollThreshold) {
                        //  Only check if right dragging
                        scrollingDirection = DIRECTION_HORIZONTAL;

                        //  articlesview automatically non-scrollable as categories covers it

                    } else if (Math.abs(deltaYInitial) >= scrollThreshold) {
                        scrollingDirection = DIRECTION_VERTICAL;
                        articlesView.setScrollable(true);
                    }

                    if (scrollingDirection != NONE){
                        articlesView.releasePendingSelectedArticle();
                    }

                } else if (scrollingDirection == DIRECTION_HORIZONTAL) {
                    float fraction = deltaXInitial / categoriesSwipeDistance;

                    if (wasCategoriesVisible) {
                        fraction += 1;
                    }

                    fraction = Math.max(0, Math.min(fraction, 1));

                    categoriesView.setTransitionValue(fraction);
                    //return false;
                }

                break;

            case MotionEvent.ACTION_UP:
                if (scrollingDirection == DIRECTION_HORIZONTAL) {
                    //  Was transitioning categories visibility
                    categoriesView.determineVisibility();
                }

                //  Enables other views to check scrolling direction
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollingDirection = NONE;
                    }
                });
                break;
        }

        return super.dispatchTouchEvent(e);
    }
}
