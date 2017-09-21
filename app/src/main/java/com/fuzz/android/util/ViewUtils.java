package com.fuzz.android.util;

import android.view.View;

/**
 * View utilities.
 */
public abstract class ViewUtils {
    private ViewUtils() {
    }

    public static void setEnabled(View v, boolean enabled) {
        if (v.isEnabled() == enabled) {
            setEnabled(v, enabled, false);
            return;
        }

        setEnabled(v, enabled, true);
    }


    public static void setEnabled(View v, boolean enabled, boolean animate) {
        v.setAlpha(enabled ? 0.75f : 1);

        float scaleTo = enabled ? 1 : 0.75f;
        float scaleFrom = enabled ? 0.75f : 1;

        v.setScaleX(scaleFrom);
        v.setScaleY(scaleFrom);

        v.animate()
                .scaleX(scaleTo)
                .scaleY(scaleTo)
                .alpha(enabled ? 1 : 0.75f)
                .start();
    }
}
