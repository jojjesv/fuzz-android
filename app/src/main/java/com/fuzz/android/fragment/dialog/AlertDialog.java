package com.fuzz.android.fragment.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.view.DefaultTypefaces;

/**
 * A custom alert dialog.
 */
public class AlertDialog extends DialogFragment {
    private DialogActions actions;

    private String header;
    private String text;

    public AlertDialog(@StringRes int header, @StringRes int text, @Nullable DialogActions actions) {
        this.actions = actions;
        this.header = getString(header);
        this.text = getString(text);
    }

    public AlertDialog(String header, String text, @Nullable DialogActions actions) {
        this.actions = actions;
        this.header = header;
        this.text = text;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.alert_dialog, container, false);

        ((TextView) root.findViewById(android.R.id.title)).setText(header);
        ((TextView) root.findViewById(android.R.id.text1)).setText(text);

        if (actions != null) {
            ViewGroup actionsContainer = (ViewGroup) root.findViewById(R.id.actions_container);

            View inflated = inflater.inflate(actions.layoutId, actionsContainer, false);
            actionsContainer.addView(inflated);

            actions.onSetupView(this, actionsContainer, inflated);
        } else {
            getDialog().setCanceledOnTouchOutside(false);
        }

        DefaultTypefaces.applyDefaultsToChildren((ViewGroup) root);

        return root;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);

        //d.getWindow().getAttributes().windowAnimations = R.style.AlertDialogAnimations;

        return d;
    }
}
