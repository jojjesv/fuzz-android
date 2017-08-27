package com.fuzz.android.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ListView;

import com.fuzz.android.R;
import com.fuzz.android.animator.AnimatorAdapter;

/**
 * View for listing categories.
 */
public class CategoriesView extends ListView {
    private Interpolator childTranslateInterpolator;
    private Interpolator visibilityInterpolator;
    private float transitionValue;
    /**
     * R, G and B.
     */
    private int[] backgroundColor;
    private ArticlesContainerView container;

    public CategoriesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();
        childTranslateInterpolator = new DecelerateInterpolator();
        visibilityInterpolator = new AccelerateDecelerateInterpolator();

        onItemsHidden();

        int bgColor = res.getColor(R.color.orange);
        backgroundColor = new int[] {
                Color.red(bgColor),
                Color.green(bgColor),
                Color.blue(bgColor)
        };
    }

    public ArticlesContainerView getContainer() {
        return container;
    }

    public void setContainer(ArticlesContainerView container) {
        this.container = container;
    }

    /**
     * Updates the transition which shows children.
     *
     * @param val Non-interpolated transition fraction.
     */
    public void setTransitionValue(float val) {
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }

        this.transitionValue = Math.max(0, Math.min(val, 1));
        val = childTranslateInterpolator.getInterpolation(val);

        float translationX;
        View child;

        for (int i = 0, n = getChildCount(); i < n; i++) {
            child = getChildAt(i);
            translationX = -child.getMeasuredWidth() * (1 - val);
            child.setTranslationX(translationX);
        }

        int newColor = Color.argb((int)(180 * val), backgroundColor[0], backgroundColor[1], backgroundColor[2]);
        setBackgroundColor(newColor);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP && container.getScrollingDirection() == ArticlesContainerView.NONE) {
            maybeHideFromTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void maybeHideFromTouch(MotionEvent ev) {
        //  Pixels from the view where touch is ignored
        int touchDeadZone = (int) (24 * getResources().getDisplayMetrics().density);
        int[] viewLocation = new int[2];
        int touchX = (int) ev.getRawX();
        int touchY = (int) ev.getRawY();
        View v;
        for (int i = 0, n = getChildCount(); i < n; i++) {
            v = getChildAt(i);
            v.getLocationInWindow(viewLocation);
            if (touchX > viewLocation[0] - touchDeadZone && touchY > viewLocation[1] - touchDeadZone
                    && touchX < viewLocation[0] + v.getMeasuredWidth() + touchDeadZone &&
                    touchY < viewLocation[1] + v.getMeasuredHeight() + touchDeadZone) {
                return;
            }
        }

        //  No views touched
        changeVisibility(true);
    }

    public boolean isVisible() {
        return transitionValue > 0;
    }

    public void determineVisibility() {
        changeVisibility(transitionValue < 0.5f);
    }

    public void changeVisibility(final boolean hide) {
        ValueAnimator animator = ValueAnimator.ofFloat(transitionValue, hide ? 0 : 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setTransitionValue((float) valueAnimator.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (hide) {
                    onItemsHidden();
                } else {
                    onItemsShown();
                }
            }
        });
        animator.setInterpolator(visibilityInterpolator);
        animator.start();
    }

    private void onItemsHidden() {
        //  Mustn't obstruct touch
        setVisibility(GONE);
    }

    private void onItemsShown() {
    }
}
