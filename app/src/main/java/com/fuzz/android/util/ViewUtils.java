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
        float scaleTo = enabled ? 1 : 0.75f;
        float scaleFrom = enabled ? 0.8f : 1;
        float alphaTo = enabled ? 1 : 0.8f;

        v.setEnabled(enabled);

        if (animate) {
            v.setAlpha(enabled ? 0.75f : 1);
            //v.setScaleX(scaleFrom);
            //v.setScaleY(scaleFrom);

            v.animate()
//                    .scaleX(scaleTo)
//                    .scaleY(scaleTo)
                    .alpha(alphaTo)
                    .start();
        } else {
//            v.setScaleX(scaleTo);
//            v.setScaleY(scaleTo);
            v.setAlpha(alphaTo);
        }
    }
}
