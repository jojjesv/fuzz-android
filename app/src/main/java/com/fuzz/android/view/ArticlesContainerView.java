package com.fuzz.android.view;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
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
    private boolean wasCategoriesVisible;

    public ArticlesContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        //  Cache resources
        Resources res = context.getResources();
        scrollThreshold = res.getDimensionPixelSize(R.dimen.scroll_threshold);
        categoriesSwipeDistance = res.getDimensionPixelSize(R.dimen.categories_swipe_distance);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        categoriesView = (CategoriesView) findViewById(R.id.categories);
        articlesView = (ArticlesView) findViewById(R.id.articles);

        articlesView.setContainer(this);
    }

    public byte getScrollingDirection(){
        return scrollingDirection;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        int action = e.getAction();

        float x = e.getRawX();
        float y = e.getRawY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = x;
                initialTouchY = y;
                articlesView.setScrollable(false);
                wasCategoriesVisible = categoriesView.isVisible();
                break;

            case MotionEvent.ACTION_MOVE:

                if (articlesView.isDraggingArticle()) {
                    break;
                }

                float deltaXInitial = x - initialTouchX;
                float deltaYInitial = y - initialTouchY;
                if (scrollingDirection == NONE) {
                    if (Math.abs(deltaXInitial) >= scrollThreshold) {
                        scrollingDirection = DIRECTION_HORIZONTAL;
                        articlesView.setScrollable(false);
                    } else if (Math.abs(deltaYInitial) >= scrollThreshold) {
                        scrollingDirection = DIRECTION_VERTICAL;
                        articlesView.setScrollable(true);
                    }
                } else if (scrollingDirection == DIRECTION_HORIZONTAL) {
                    float fraction = deltaXInitial / categoriesSwipeDistance;

                    if (wasCategoriesVisible) {
                        fraction += 1;
                    }

                    fraction = Math.max(0, Math.min(fraction, 1));

                    categoriesView.setTransitionValue(fraction);
                    return false;
                }

                break;

            case MotionEvent.ACTION_UP:
                if (scrollingDirection == DIRECTION_HORIZONTAL) {
                    //  Was transitioning categories visibility
                    categoriesView.determineVisibility();
                }

                articlesView.setScrollable(false);
                scrollingDirection = NONE;
                break;
        }

        return super.onInterceptTouchEvent(e);
    }
}
