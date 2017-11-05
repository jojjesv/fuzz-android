package com.fuzz.android.view;

import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.fuzz.android.R;

/**
 * Timer which ticks down along with delivery ETA of an order.
 */
public class OrderEtaTimer extends TextView {

    private StringBuilder timerTextBuilder = new StringBuilder();

    private int innerRadius;
    private ValueAnimator countdownAnimator;
    private Paint arcPaint;
    private OnReachedZeroListener reachedZeroListener;
    private RectF arcOval;

    public OrderEtaTimer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OnReachedZeroListener getReachedZeroListener() {
        return reachedZeroListener;
    }

    public void setReachedZeroListener(OnReachedZeroListener reachedZeroListener) {
        this.reachedZeroListener = reachedZeroListener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean editMode = isInEditMode();

        if (countdownAnimator != null || editMode) {
            if (arcOval == null){
                int mwidth = getMeasuredWidth();
                int mheight = getMeasuredHeight();

                float padding = 0;

                arcOval = new RectF(padding, padding, mwidth - padding * 2, mheight - padding * 2);
            }
            float degrees = editMode ? 315 : (float) countdownAnimator.getAnimatedValue();

            if (arcPaint == null){
                //  Has layout
                Resources res = getResources();
                Resources.Theme theme = getContext().getTheme();

                arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                arcPaint.setShader(
                        new LinearGradient(0,
                                0, arcOval.width(), arcOval.height(),
                                ResourcesCompat.getColor(res, R.color.light_blue, theme),
                                ResourcesCompat.getColor(res, R.color.red, theme),
                                Shader.TileMode.CLAMP)
                );
            }

            canvas.drawArc(arcOval, 270, -degrees, true, arcPaint);

            //canvas.drawCircle(mwidth * 0.5f, mheight * 0.5f, innerRadius, innerPaint);
        }
        super.onDraw(canvas);
    }

    private void onCountdownTick(int secondsRemaining) {
        int secondsToDisplay = secondsRemaining % 60;
        int minutesToDisplay = (int) Math.floor((float) secondsRemaining / 60);

        timerTextBuilder.setLength(0);
        if (minutesToDisplay < 10) {
            timerTextBuilder.append('0');
        }
        timerTextBuilder.append(minutesToDisplay).append(':');

        if (secondsToDisplay < 10) {
            timerTextBuilder.append('0');
        }
        timerTextBuilder.append(secondsToDisplay);

        setText(timerTextBuilder.toString());
    }

    public void startCountdown(final int seconds, final int secondsPassed) {
        if (countdownAnimator != null) {
            //  Already started
            return;
        }

        //  https://stackoverflow.com/questions/36647444/how-to-read-animation-scale-duration-flag-in-marshmallow
        ContentResolver cr = getContext().getContentResolver();
        float animatorSpeed;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            animatorSpeed = Settings.Global.getFloat(cr,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1);
        }
        else
        {
            animatorSpeed = Settings.System.getFloat(cr,
                    Settings.System.ANIMATOR_DURATION_SCALE,
                    1);
        }

        countdownAnimator = ValueAnimator.ofFloat((int)(360f * (1 - ((float)secondsPassed / seconds))), 0);
        countdownAnimator.setDuration((long)((1000 * seconds) / animatorSpeed));
        countdownAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //  TODO: Calculate dirty area
                postInvalidate();
                Log.d("Timer", "Play time: " + valueAnimator.getCurrentPlayTime() + ", fraction: " + valueAnimator.getAnimatedFraction());
            }
        });
        countdownAnimator.setInterpolator(new LinearInterpolator());
        countdownAnimator.start();

        final android.os.Handler ticker = new Handler();
        ticker.post(new Runnable() {
            int secondsRemaining = seconds - secondsPassed;

            @Override
            public void run() {
                if (secondsRemaining > 0) {
                    onCountdownTick(--secondsRemaining);
                    ticker.postDelayed(this, 1000);
                } else {
                    reachedZeroListener.onReachedZero(OrderEtaTimer.this);
                }
            }
        });
    }

    public interface OnReachedZeroListener {
        void onReachedZero(OrderEtaTimer timer);
    }
}
