package com.fuzz.android.fragment;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.view.DefaultTypefaces;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Shows information about both the app and the company.
 */
public class AboutAppFragment extends BaseDialogFragment {

    private static String companyName;
    private static String companyEmail;
    private static String companyPhoneNum;
    private static String companyAddress;

    public static void setFromConfig(JSONObject config) {
        try {
            companyName = config.getString("company_name");
            companyEmail = config.getString("company_email");
            companyPhoneNum = config.getString("company_phone_num");
            companyAddress = config.getString("company_address");
        } catch (JSONException ex) {

        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.about_app_layout, container, false);
        setupView(v);
        return v;
    }

    private void setupView(View v) {
        ((TextView) v.findViewById(R.id.company_name)).setText(companyName);
        ((TextView) v.findViewById(R.id.phone_number)).setText(companyPhoneNum);
        ((TextView) v.findViewById(R.id.label_address)).setText(companyAddress);
        ((TextView) v.findViewById(R.id.email)).setText(companyEmail);

        //  Close button
        v.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        String appVersion = "";
        try {

            Context context = getActivity();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException ex) {

        }

        ((TextView) v.findViewById(R.id.app_version)).setText(getString(R.string.version, appVersion));

        DefaultTypefaces.applyDefaultsToChildren((ViewGroup) v);
    }
}
