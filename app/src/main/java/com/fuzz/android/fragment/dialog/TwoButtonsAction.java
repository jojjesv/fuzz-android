package com.fuzz.android.fragment.dialog;

import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fuzz.android.R;

/**
 * Dialog action by two buttons.
 */
public class TwoButtonsAction extends DialogActions {
    private Runnable acceptTask;
    private int acceptText;
    public boolean dismissOnAccept = true;

    public TwoButtonsAction(@StringRes int acceptText, Runnable acceptTask) {
        super(R.layout.dialog_actions_two_buttons);
        this.acceptTask = acceptTask;
        this.acceptText = acceptText;
    }

    @Override
    void onSetupView(AlertDialog dialog, ViewGroup container, View inflatedView) {
        final AlertDialog DIALOG = dialog;
        container.findViewById(R.id.accept_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dismissOnAccept){
                    DIALOG.dismiss();
                }

                acceptTask.run();
            }
        });
        ((TextView)container.findViewById(R.id.accept_text)).setText(this.acceptText);
        
        container.findViewById(R.id.decline_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DIALOG.dismiss();
            }
        });
    }
}
