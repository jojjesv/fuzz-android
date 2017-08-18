package com.fuzz.android.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
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

    public CategoriesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();
        childTranslateInterpolator = new DecelerateInterpolator();
        visibilityInterpolator = new AccelerateDecelerateInterpolator();

        onItemsHidden();
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

        this.transitionValue = val;

        float translationX;
        View child;

        float a = 0;
        for (int i = 0, n = getChildCount(); i < n; i++) {
            child = getChildAt(i);
            translationX = -child.getMeasuredWidth() * (1 - childTranslateInterpolator.getInterpolation(Math.max(0, Math.min((val * 0.17447917f - (float) (i * 0.1f) / n) * n, 1))));
            child.setTranslationX(translationX);
            a = translationX;
        }
    }

    public boolean isVisible(){
        return transitionValue > 0;
    }

    public void determineVisibility() {
        final boolean hide = transitionValue < 0.5f;

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

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.i("cat", ev.toString());
        return super.onTouchEvent(ev);
    }
}
