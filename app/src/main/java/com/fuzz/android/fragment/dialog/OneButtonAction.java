package com.fuzz.android.fragment.dialog;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fuzz.android.R;

/**
 * Dialog action by two buttons.
 */
public class OneButtonAction extends DialogActions {
    private final Runnable acceptTask;
    private final int acceptText;

    public OneButtonAction(@StringRes int acceptText, @Nullable Runnable acceptTask) {
        super(R.layout.dialog_actions_one_button);
        this.acceptTask = acceptTask;
        this.acceptText = acceptText;
    }

    @Override
    void onSetupView(AlertDialog dialog, ViewGroup container, View inflatedView) {
        final AlertDialog DIALOG = dialog;
        inflatedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (acceptTask == null){
                    DIALOG.dismiss();
                } else {
                    acceptTask.run();
                }
            }
        });
        ((TextView)container.findViewById(R.id.accept_text)).setText(this.acceptText);
    }
}
