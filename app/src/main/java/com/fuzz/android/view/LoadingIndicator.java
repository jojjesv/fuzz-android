package com.fuzz.android.view;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

/**
 * View for indicating a loading task.
 */
public class LoadingIndicator extends AppCompatImageView {
    private boolean startedAnimation;
    private RotateAnimation animation;

    public LoadingIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void startDefaultAnimation(){
        RotateAnimation rotateAnim = animation != null ? animation : new RotateAnimation(359, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnim.setRepeatCount(Animation.INFINITE);
        rotateAnim.setDuration(750);
        rotateAnim.setInterpolator(new LinearInterpolator());
        startAnimation(rotateAnim);

        animation = rotateAnim;
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == GONE || visibility == INVISIBLE) {
            clearAnimation();
        } else {
            startDefaultAnimation();
        }

        super.setVisibility(visibility);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!startedAnimation){
            startDefaultAnimation();
            startedAnimation = true;
        }
    }
}
