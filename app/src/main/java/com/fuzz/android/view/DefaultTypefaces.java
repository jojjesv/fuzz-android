package com.fuzz.android.view;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Default typefaces.
 */
public class DefaultTypefaces {
    private static Typeface defaultText;
    private static Typeface defaultHeader;

    public static Typeface getDefaultText() {
        return defaultText;
    }

    public static Typeface getDefaultHeader() {
        return defaultHeader;
    }

    public static void setup(Resources res) {
        AssetManager assets = res.getAssets();
        defaultText = Typeface.createFromAsset(assets, "fonts/merge_light.ttf");
        defaultHeader = Typeface.createFromAsset(assets, "fonts/caveat-bold.ttf");
    }

    /**
     * Applies a specific typeface to all views inside an activity.
     *
     * @param typeface
     * @param activity
     */
    public static void applyToViews(Typeface typeface, Activity activity) {
        applyToChildren(typeface, null, (ViewGroup) activity.findViewById(android.R.id.content));
    }

    public static void applyDefaultsToViews(Activity activity) {
        applyDefaultsToChildren((ViewGroup) activity.findViewById(android.R.id.content));
    }

    public static void applyDefaultsToChildren(ViewGroup parent) {
        applyToChildren(getDefaultText(), getDefaultHeader(), parent);
    }

    public static void applyToChildren(Typeface typefaceDefault, Typeface typefaceHeader, ViewGroup parent) {
        View v;
        TextView tv;
        for (int i = 0, n = parent.getChildCount(); i < n; i++) {
            v = parent.getChildAt(i);

            if (v instanceof ViewGroup) {
                applyToChildren(typefaceDefault, typefaceHeader, (ViewGroup) v);
            }

            if (v instanceof TextView) {
                tv = (TextView) v;

                if (typefaceHeader != null && tv instanceof HeaderTextView) {
                    tv.setTypeface(typefaceHeader);
                } else {
                    tv.setTypeface(typefaceDefault);
                }
            }
        }
    }
}
