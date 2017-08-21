package com.fuzz.android.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * View presenting an article.
 */
public class ArticleView extends LinearLayout {
    private static final float TOUCH_DOWN_ALPHA = 0.7f;

    /**
     * Whether is picked up by the user.
     */
    private boolean isPickedUp;

    public ArticleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isPickedUp() {
        return isPickedUp;
    }

    public void setPickedUp(boolean pickedUp) {
        isPickedUp = pickedUp;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            setAlpha(TOUCH_DOWN_ALPHA);
        } else if (action == MotionEvent.ACTION_UP) {
            if (!isPickedUp) {
                setAlpha(1);
            } else {
                //  Don't set alpha to 1 when up, alpha is managed by articlesview
                isPickedUp = false;
            }
        }

        return super.dispatchTouchEvent(ev);
    }
}
