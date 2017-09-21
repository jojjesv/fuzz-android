package com.fuzz.android.helper;

import android.app.Activity;

import com.fuzz.android.fragment.AboutAppFragment;
import com.fuzz.android.fragment.FeedbackFragment;

/**
 * Provides functionality for the about footer.
 */
public class AboutFooterHelper {
    private static AboutFooterHelper instance;

    public static AboutFooterHelper getInstance() {
        if (instance == null) {
            instance = new AboutFooterHelper();
        }
        return instance;
    }

    public void showAboutApp(Activity activity) {
        AboutAppFragment dialog = new AboutAppFragment();
        dialog.show(activity.getFragmentManager(), null);
    }

    public void showFeedbackDialog(Activity activity) {
        FeedbackFragment dialog = new FeedbackFragment();
        dialog.setActivity(activity);
        dialog.show(activity.getFragmentManager(), null);
    }
}
