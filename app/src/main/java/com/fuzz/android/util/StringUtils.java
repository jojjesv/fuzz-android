package com.fuzz.android.util;

import android.content.Context;

import com.fuzz.android.R;

/**
 * String utilities.
 */
public abstract class StringUtils {
    private StringUtils() {

    }

    /**
     * Glues items to a comma-delimited list.
     *
     * @param items
     * @return
     */
    public static String glue(Context context, String... items) {
        if (items.length == 1) {
            return items[0];
        }
        StringBuilder output = new StringBuilder();

        for (int i = 0, n = items.length - 1; i <= n; i++) {
            if (i == n) {
                //  Last itr
                output.append(' ').append(context.getString(R.string.and)).append(' ');
            }

            output.append(items[i]);

            if (i < n - 1) {
                output.append(", ");
            }
        }

        return output.toString();
    }
}
