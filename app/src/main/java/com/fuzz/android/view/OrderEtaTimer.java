package com.fuzz.android.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
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
    private Paint innerPaint;
    private OnReachedZeroListener reachedZeroListener;
    private RectF arcOval;

    public OrderEtaTimer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(context.getResources().getColor(R.color.red));

        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setColor(Color.WHITE);

        Resources res = context.getResources();
        innerRadius = res.getDimensionPixelSize(R.dimen.eta_timer_inner_radius);
        setTypeface(Typeface.createFromAsset(res.getAssets(), "fonts/segment7standard.ttf"));
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
                arcOval = new RectF(0, 0, mwidth, mheight);
            }
            float degrees = editMode ? 20 : (float) countdownAnimator.getAnimatedValue();
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

        countdownAnimator = ValueAnimator.ofFloat((int)(360f * (1 - ((float)secondsPassed / seconds))), 0);
        countdownAnimator.setDuration(2000 * seconds);
        countdownAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //  TODO: Calculate dirty area
                postInvalidate();
                //Log.d("Timer", "Play time: " + valueAnimator.getCurrentPlayTime() + ", fraction: " + valueAnimator.getAnimatedFraction());
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
        public void onReachedZero(OrderEtaTimer timer);
    }
}
