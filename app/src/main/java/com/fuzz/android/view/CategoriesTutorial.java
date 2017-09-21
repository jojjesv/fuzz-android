package com.fuzz.android.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.fuzz.android.R;
import com.fuzz.android.activity.MainActivity;
import com.fuzz.android.animator.AnimatorAdapter;

import android.os.Handler;
import android.widget.TextView;

/**
 * Instructs the user on how to show the list of categories.
 */
public class CategoriesTutorial {
    private ImageView touchView;
    private CategoriesView categoriesView;
    private Handler handler;
    private boolean isHidingTouch;
    private TextView label;
    private ViewGroup root;
    private MainActivity activity;

    /**
     * Starts the tutorial.
     *
     * @param activity
     */
    public CategoriesTutorial(MainActivity activity) {
        handler = new Handler();
        this.activity = activity;
        root = (ViewGroup) activity.findViewById(R.id.root_frame);
        categoriesView = (CategoriesView) root.findViewById(R.id.categories);
        delaySetupViews(activity);
        startObstructTouch();
    }

    private void startObstructTouch() {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void stopObstructTouch() {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void setupViews(Activity activity) {
        final Resources res = activity.getResources();

        touchView = new ImageView(activity);
        touchView.setImageBitmap(BitmapFactory.decodeResource(res, R.drawable.ic_touch));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT | Gravity.CENTER;
        touchView.setLayoutParams(params);

        root.addView(touchView);

        label = (TextView) LayoutInflater.from(activity).inflate(R.layout.categories_label, root, false);
        root.addView(label);
    }

    private void delaySetupViews(final Activity activity) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setupViews(activity);
                animateViews(activity);
            }
        }, 2000);
    }

    private void animateViews(Activity activity) {
        final Resources res = activity.getResources();

        final float touchViewTranslationX = res.getDimension(R.dimen.category_touch_x);
        touchView.setTranslationX(touchViewTranslationX);

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

        label.setAlpha(0);
        label.animate()
                .alpha(1)
                .start();

        delayLabelTextChange();
    }

    private void delayLabelTextChange() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeLabelText();
            }
        }, 4000);
    }

    private void changeLabelText() {
        label.animate()
                .alpha(0)
                .setListener(new AnimatorAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        label.setText(R.string.swipe_categories_hide);
                        label.animate()
                                .alpha(1)
                                .start();
                    }
                })
                .start();
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
                hideViews();
            }
        }, 2500);
    }

    private void hideViews() {
        ViewPropertyAnimator touchAnim = touchView.animate()
                .alpha(0)
                .setListener(new AnimatorAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        ((ViewGroup) touchView.getParent()).removeView(touchView);
                        ((ViewGroup) label.getParent()).removeView(label);
                        stopObstructTouch();
                    }
                });
        label.animate()
                .setDuration(touchAnim.getDuration())
                .alpha(0)
                .start();

        touchAnim.start();
    }
}
