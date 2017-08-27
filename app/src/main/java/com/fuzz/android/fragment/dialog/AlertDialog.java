package com.fuzz.android.fragment.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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

    public AlertDialog(Context context, @StringRes int header, @StringRes int text, @Nullable DialogActions actions) {
        this.actions = actions;
        this.header = context.getString(header);
        this.text = context.getString(text);
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

        Resources res = getResources();
        dialog.getWindow().setLayout(res.getDimensionPixelSize(R.dimen.dialog_width),
                res.getDimensionPixelSize(R.dimen.dialog_height));

        return dialog;
    }
}
