package com.fuzz.android.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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

    public OrderEtaTimer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(Color.GREEN);

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
        if (countdownAnimator != null) {
            float degrees = isInEditMode() ? 220 : (float) countdownAnimator.getAnimatedValue();
            int mwidth = getMeasuredWidth();
            int mheight = getMeasuredHeight();
            canvas.drawArc(0, 0, mwidth, mheight, 270, degrees, true, arcPaint);

            canvas.drawCircle(mwidth * 0.5f, mheight * 0.5f, innerRadius, innerPaint);
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

    public void startCountdown(int minutes) {
        if (countdownAnimator != null) {
            //  Already started
            return;
        }

        countdownAnimator = ValueAnimator.ofFloat(360, 0);
        countdownAnimator.setDuration(60000 * minutes);
        countdownAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //  TODO: Calculate dirty area
                postInvalidate();
            }
        });
        countdownAnimator.setInterpolator(new LinearInterpolator());
        countdownAnimator.start();

        final int MINS = minutes;
        final android.os.Handler ticker = new Handler();
        ticker.post(new Runnable() {
            int secondsRemaining = MINS * 60;

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
