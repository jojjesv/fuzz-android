package com.fuzz.android.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.fuzz.android.R;

/**
 * Created by Johan on 2017-07-31.
 */

public class AnimatedBackgroundCircle extends View {
    public AnimatedBackgroundCircle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.AnimatedBackgroundCircle);
        final float translationX = attributes.getDimension(R.styleable.AnimatedBackgroundCircle_translation_x_to, 0);
        final float translationY = attributes.
                getDimension(R.styleable.AnimatedBackgroundCircle_translation_y_to, 0);

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(attributes.getInteger(R.styleable.AnimatedBackgroundCircle_translation_duration, 60000));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private boolean hasFromLocation;
            float fromX, fromY;

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (!hasFromLocation) {
                    fromX = getTranslationX();
                    fromY = getTranslationY();
                    hasFromLocation = true;
                }

                float val = (float) Math.sin(Math.PI * valueAnimator.getAnimatedFraction());
                setTranslationX(fromX + (translationX - fromX) * val);
                setTranslationY(fromY + (translationY - fromY) * val);
            }
        });
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }
}
