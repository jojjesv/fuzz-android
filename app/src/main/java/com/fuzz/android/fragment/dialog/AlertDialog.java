package com.fuzz.android.fragment.dialog;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.fragment.BaseDialogFragment;
import com.fuzz.android.view.DefaultTypefaces;

/**
 * A custom alert dialog.
 */
public class AlertDialog extends BaseDialogFragment {
    private DialogActions actions;

    private String header;
    private String text;

    private View view;

    public AlertDialog() {
    }

    public AlertDialog setHeader(String header) {
        this.header = header;
        return this;
    }

    public AlertDialog setText(String text) {
        this.text = text;
        return this;
    }

    public AlertDialog setActions(@Nullable DialogActions actions) {
        this.actions = actions;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = this.view = inflater.inflate(R.layout.alert_dialog, null, false);

        ((TextView) root.findViewById(android.R.id.title)).setText(header);
        ((TextView) root.findViewById(R.id.text)).setText(text);

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
        Dialog dialog = super.onCreateDialog(savedInstanceState);


        return dialog;
    }
}
