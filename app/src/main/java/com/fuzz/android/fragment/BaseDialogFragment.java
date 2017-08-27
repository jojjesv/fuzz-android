package com.fuzz.android.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.Window;

import com.fuzz.android.R;

/**
 * Default "speech bubble" dialog style.
 */
public class BaseDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        Window window = dialog.getWindow();
        window.getDecorView().setBackground(null);
        window.getAttributes().windowAnimations = R.style.InfoDialogAnimations;

        return dialog;
    }

}
