package com.fuzz.android.view;

import android.animation.ValueAnimator;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.fuzz.android.R;

/**
 * Helper for animating the truck drawable.
 */
public abstract class TruckAnimator {
    private TruckAnimator() {

    }

    public static void animate(final View truck) {
        final View base = truck.findViewById(R.id.base);
        final View leftWheel = truck.findViewById(R.id.left_wheel);
        final View rightWheel = truck.findViewById(R.id.right_wheel);

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float baseDeltaY = 2 * truck.getResources().getDisplayMetrics().density;

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();
                int wheelDegrees = (int) (-360 * val);

                leftWheel.setRotation(wheelDegrees);
                rightWheel.setRotation(wheelDegrees);

                base.setTranslationY(-baseDeltaY * (float) Math.sin(Math.PI * val));
            }
        });
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }
}
