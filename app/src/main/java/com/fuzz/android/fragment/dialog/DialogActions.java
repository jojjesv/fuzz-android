package com.fuzz.android.fragment.dialog;

import android.support.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;

/**
 * Base class for defining a set of dialog action controls.
 */
public abstract class DialogActions {
    int layoutId;

    DialogActions(@LayoutRes int layoutId){
        this.layoutId = layoutId;
    }

    abstract void onSetupView(AlertDialog dialog, ViewGroup container, View inflatedView);
}
