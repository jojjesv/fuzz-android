package com.fuzz.android.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.fuzz.android.R;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.util.ViewUtils;

/**
 * Dialog fragment for sending feedback.
 */
public class FeedbackFragment extends BaseDialogFragment {
    /**
     * Needed when showing thanks dialog.
     */
    private Activity activity;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.feedback_layout, container, false);
        setupView(v);
        return v;
    }

    private void setupView(View v) {
        View submit = v.findViewById(R.id.submit);

        ViewUtils.setEnabled(submit, false, false);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendFeedback();
            }
        });
    }

    private void sendFeedback() {
        View v = getView();
        EditText messageView = (EditText) v.findViewById(R.id.message_input);

        StringBuilder postBuilder = new StringBuilder("platform=android");
        try {
            Activity a = getActivity();
            PackageManager pm = a.getPackageManager();
            PackageInfo versioning = pm.getPackageInfo(a.getPackageName(), 0);
            postBuilder.append("&app_version=").append(BackendCom.encode(versioning.versionName));
        } catch (PackageManager.NameNotFoundException ex) {

        }
        postBuilder.append("&message=").append(BackendCom.encode(messageView.getText().toString()));
        BackendCom.request("action=send_feedback", postBuilder.toString(), new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parseSendFeedbackResponse(response);
            }

            @Override
            public void onFailed() {

            }
        });
    }

    private void parseSendFeedbackResponse(String response) {
        dismiss();
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showThanks();
            }
        }, 500);
    }

    private void showThanks() {
        new FeedbackThanksFragment().show(activity.getFragmentManager(), null);
    }
}
