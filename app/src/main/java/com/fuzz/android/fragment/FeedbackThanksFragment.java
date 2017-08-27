package com.fuzz.android.fragment;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.fuzz.android.R;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.view.DefaultTypefaces;

/**
 * Dialog fragment for sending feedback.
 */
public class FeedbackThanksFragment extends BaseDialogFragment {

    public FeedbackThanksFragment() {
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.feedback_thanks, container, false);
        DefaultTypefaces.applyDefaultsToChildren((ViewGroup)v);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        delayDismiss();
    }

    private void delayDismiss() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isVisible()) {
                    dismiss();
                }
            }
        }, 2000);
    }
}
