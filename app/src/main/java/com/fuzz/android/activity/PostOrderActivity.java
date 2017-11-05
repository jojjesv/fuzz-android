package com.fuzz.android.activity;

import android.animation.Animator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.fuzz.android.R;
import com.fuzz.android.animator.AnimatorAdapter;
import com.fuzz.android.service.EtaChangeNotifier;
import com.fuzz.android.view.DefaultTypefaces;
import com.fuzz.android.view.OrderEtaTimer;

/**
 * Activity shown after ordering.
 */
public class PostOrderActivity extends Activity implements EtaChangeNotifier.EtaChangeListener, OrderEtaTimer.OnReachedZeroListener {

    private static final long BACK_PRSS_MILLIS_THRESHOLD = 1000;
    private String[] trivialMessages;
    private int trivialMessageIndex;
    private TextView trivialMessagesView;
    private long millisSinceBackPress;
    private int orderId;
    private boolean trivialMessagesActive = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_order);
        DefaultTypefaces.applyDefaultsToViews(this);

        setupCompatBackground();
        cacheResources();
        cacheAndSetupViews();

        parseIntentData();

        loopTrivialMessageUpdates();
    }

    private void setupCompatBackground(){
        if (Build.VERSION.SDK_INT < 21){
            Log.i(getClass().getSimpleName(), "Using compat background tints");

            Resources res = getResources();
            Resources.Theme theme = getTheme();
            DrawableCompat.setTint(
                    DrawableCompat.wrap(findViewById(R.id.background_circle_1).getBackground()),
                    ResourcesCompat.getColor(res, R.color.light_red, theme)
            );
            DrawableCompat.setTint(
                    DrawableCompat.wrap(findViewById(R.id.background_circle_2).getBackground()),
                    ResourcesCompat.getColor(res, R.color.lightest_blue, theme)
            );
        }
    }

    private void showThanks(){
        final View container = findViewById(R.id.thanks_container);
        final View text = container.findViewById(R.id.thanks_text);
        final View candyWrapper = container.findViewById(R.id.candy_wrapper);

        //  Duration for each container animation
        long duration1 = 750;
        long duration2 = 500;

        container.setScaleX(0.6f);
        container.setScaleY(0.6f);
        container.setVisibility(View.VISIBLE);

        container.animate()
                .setDuration(duration1)
                .setInterpolator(new OvershootInterpolator())
                .scaleX(1)
                .scaleY(1)
                .alpha(1)
                .start();

        candyWrapper.animate()
                .setDuration(duration2)
                .translationY(
                        getResources().getDimensionPixelSize(R.dimen.post_order_candy_translation)
                )
                .alpha(0)
                .rotation(25)
                .setInterpolator(new AccelerateInterpolator())
                .setStartDelay(duration1 + 250)
                .start();

        text.animate()
                .setStartDelay(duration1 + duration2 + 750)
                .alpha(1)
                .start();

        //  Hide other
        View[] toHide = new View[]{
                findViewById(R.id.header),
                findViewById(R.id.subheader),
                findViewById(R.id.trivial_messages)
        };

        trivialMessagesActive = false;

        ViewPropertyAnimator anim;
        for (final View v : toHide) {
            anim = v.animate();
            anim.cancel();
            anim.alpha(0)
                .setListener(new AnimatorAdapter(){
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        try {
                            ((ViewGroup) v.getParent()).removeView(v);
                        } catch (Exception ex){
                            //  May not have a parent
                        }
                    }
                }).start();
        }
    }

    @Override
    public void onBackPressed() {
        long millis = System.currentTimeMillis();

        if (millis - millisSinceBackPress < BACK_PRSS_MILLIS_THRESHOLD) {
            super.onBackPressed();
        } else {
            //  Not fast enough
            Toast toast = Toast.makeText(this, R.string.leave_post_order, Toast.LENGTH_LONG);
            DrawableCompat.setTint(toast.getView().getBackground(), ResourcesCompat.getColor(getResources(), R.color.light_red, getTheme()));
            toast.show();
        }

        millisSinceBackPress = millis;
    }

    /**
     * Checks if there has been a previous order.
     */
    private void parseIntentData() {
        Intent i = getIntent();
        orderId = i.getIntExtra("order_id", -1);

        if (orderId == -1) {
            //  Show current order
            long secsAtNotification = i.getLongExtra("secs_at_notif", 0);
            int etaSeconds = i.getIntExtra("eta_minutes", 0) * 60;

            int secondsPassed = (int) (System.currentTimeMillis() / 1000L - secsAtNotification);
            if (etaSeconds <= 0) {
                onReachedZero((OrderEtaTimer) findViewById(R.id.timer));
                return;
            }

            String deliverer = i.getStringExtra("deliverer");

            onEtaChange(-1, etaSeconds, secondsPassed, deliverer);

        } else {
            boolean debug = false;
            //  TODO: Funny loading messages at bottom
            if (!debug)
                startNotifierService(orderId);
            else {
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onEtaChange(1, 120, 0, "Johan");
                    }
                }, 4000);
            }
        }
    }

    private void cacheResources() {
        Resources res = getResources();
        trivialMessages = res.getStringArray(R.array.delivery_trivial_messages);
    }

    private void cacheAndSetupViews() {
        trivialMessagesView = (TextView) findViewById(R.id.trivial_messages);

        ViewCompat.setElevation(trivialMessagesView, getResources().getDimension(R.dimen.default_elevation));

        OrderEtaTimer timer = (OrderEtaTimer) findViewById(R.id.timer);
        timer.setReachedZeroListener(this);
    }

    @Override
    public void onReachedZero(OrderEtaTimer timer) {
        Interpolator hideInterpolator = new AnticipateInterpolator();
        long dur = 1000;
        timer.animate()
                .scaleX(0)
                .scaleY(0)
                .setDuration(dur)
                .setInterpolator(hideInterpolator)
                .start();

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showThanks();
            }
        }, dur + 500);
    }

    /**
     * Immediately updates the trivial message text, and delays the next update.
     */
    private void loopTrivialMessageUpdates() {
        final android.os.Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (trivialMessagesActive) {
                    trivialMessageIndex = (trivialMessageIndex + 1) % trivialMessages.length;
                    setTrivialMessage(trivialMessages[trivialMessageIndex]);
                    handler.postDelayed(this, 3500);
                }
            }
        });

        trivialMessagesView.setText(trivialMessages[trivialMessageIndex]);
    }

    private void setTrivialMessage(final String message) {
        trivialMessagesView.animate()
                .alpha(0)
                .setListener(new AnimatorAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        trivialMessagesView.setText(message);
                        trivialMessagesView.animate()
                                .alpha(1)
                                .start();
                    }
                })
                .start();
    }

    private void startNotifierService(int orderId) {
        EtaChangeNotifier.setActive(true);

        Intent serviceIntent = new Intent(this, EtaChangeNotifier.class);
        serviceIntent.putExtra("order_id", orderId);

        bindService(serviceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                ((EtaChangeNotifier.Binder) iBinder).getService().setListener(PostOrderActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onEtaChange(int orderId, int etaSeconds, int secondsPassed, String delivererName) {
        OrderEtaTimer timerView = (OrderEtaTimer) findViewById(R.id.timer);

        timerView.startCountdown(etaSeconds, secondsPassed);
        timerView.setAlpha(0f);
        timerView.setScaleX(0.6f);
        timerView.setScaleY(0.6f);
        timerView.animate()
                .scaleX(1)
                .scaleY(1)
                .alpha(1f)
                .start();

        timerView.setVisibility(View.VISIBLE);

        final String DELIVERER = delivererName;

        final TextView subheader = (TextView) findViewById(R.id.subheader);
        subheader.animate()
                .alpha(0)
                .setListener(new AnimatorAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        subheader.setText(getString(R.string.post_order_subheader_confirmed, DELIVERER));
                        subheader.animate()
                                .alpha(1)
                                .setListener(null)
                                .start();
                    }
                }).start();
    }
}
