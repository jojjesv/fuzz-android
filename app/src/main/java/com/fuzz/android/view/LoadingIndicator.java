package com.fuzz.android.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

/**
 * View for indicating a loading task.
 */
public class LoadingIndicator extends AppCompatImageView {
    private boolean startedAnimation;

    public LoadingIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private void startAnimation(){
        RotateAnimation rotateAnim = new RotateAnimation(359, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnim.setRepeatCount(Animation.INFINITE);
        rotateAnim.setDuration(750);
        rotateAnim.setInterpolator(new LinearInterpolator());
        startAnimation(rotateAnim);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!startedAnimation){
            startAnimation();
            startedAnimation = true;
        }
    }
}
