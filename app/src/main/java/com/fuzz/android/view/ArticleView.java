package com.fuzz.android.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
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

    public void setHoverEffectEnabled(boolean enabled){
        setAlpha(enabled ? TOUCH_DOWN_ALPHA : 1);
    }
}
