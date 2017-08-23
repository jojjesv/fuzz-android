package com.fuzz.android.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.fuzz.android.R;
import com.fuzz.android.activity.MainActivity;
import com.fuzz.android.animator.AnimatorAdapter;

import android.os.Handler;

/**
 * Instructs the user on how to show the list of categories.
 */
public class CategoriesTutorial {
    private ImageView touchView;
    private CategoriesView categoriesView;
    private Handler handler;
    private boolean isHidingTouch;

    /**
     * Starts the tutorial.
     *
     * @param activity
     */
    public CategoriesTutorial(MainActivity activity) {
        handler = new Handler();
        categoriesView = (CategoriesView) activity.findViewById(R.id.categories);
        setupTouchView(activity);
    }

    private void setupTouchView(Activity activity) {
        final Resources res = activity.getResources();

        touchView = new ImageView(activity);
        touchView.setImageBitmap(BitmapFactory.decodeResource(res, R.drawable.ic_touch));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT | Gravity.CENTER;
        touchView.setLayoutParams(params);

        final float touchViewTranslationX = res.getDimension(R.dimen.category_touch_x);
        touchView.setTranslationX(touchViewTranslationX);

        ((ViewGroup) activity.findViewById(R.id.root_frame)).addView(touchView);

        //  Animate
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float deltaX = res.getDimension(R.dimen.categories_swipe_distance);

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();
                touchView.setTranslationX(touchViewTranslationX + deltaX * val);
                categoriesView.setTransitionValue(val);
            }
        });
        animator.addListener(new AnimatorAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (isHidingTouch) {
                    delayTouchHide();
                } else {
                    reverseTouchAnimation((ValueAnimator) animator);
                }
            }
        });
        animator.setDuration(1500);
        animator.start();
    }

    private void reverseTouchAnimation(final ValueAnimator original) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                original.reverse();
            }
        }, 3000);
        isHidingTouch = true;
    }

    private void delayTouchHide() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideTouch();
            }
        }, 2500);
    }

    private void hideTouch() {
        touchView.animate()
                .alpha(0)
                .setListener(new AnimatorAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        ((ViewGroup)touchView.getParent()).removeView(touchView);
                    }
                })
                .start();
    }
}
