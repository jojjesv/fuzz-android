package com.fuzz.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;

import com.fuzz.android.R;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.fragment.AboutAppFragment;
import com.fuzz.android.fragment.dialog.AlertDialog;
import com.fuzz.android.fragment.dialog.OneButtonAction;
import com.fuzz.android.helper.AboutFooterHelper;
import com.fuzz.android.preferences.PreferenceKeys;
import com.fuzz.android.view.DefaultTypefaces;
import com.fuzz.android.view.TruckAnimator;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity for splash and postal code input.
 */
public class PostalCodeActivity extends Activity {
    private String lastSubmittedPostalCode;
    private SharedPreferences preferences;
    private android.os.Handler handler;
    private Interpolator viewInterpolator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DefaultTypefaces.setup(getResources());

        handler = new Handler();
        viewInterpolator = new OvershootInterpolator();

        setContentView(R.layout.activity_postal_code);
        DefaultTypefaces.applyDefaultsToViews(this);

        setupLayout();
        animateLayout();

        setupPreferences();

        fetchConfig();
    }

    private void setupLayout() {
        TruckAnimator.animate(findViewById(R.id.truck));
    }

    private void animateLayout() {
        int baseDelay = 200;
        animateView(findViewById(R.id.header), 0);
        animateView(findViewById(R.id.subheader), baseDelay);
        animateView(findViewById(R.id.text_input), baseDelay * 2);
        animateView(findViewById(R.id.submit), baseDelay * 3);
        animateTruckView(findViewById(R.id.truck), baseDelay * 5);
    }

    private void animateView(final View v, int delay) {
        v.setVisibility(View.INVISIBLE);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.setVisibility(View.VISIBLE);
                v.setAlpha(0);
                v.setScaleX(0.6f);
                v.setScaleY(0.6f);

                v.animate()
                        .alpha(1)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500)
                        .setInterpolator(viewInterpolator);
            }
        }, delay);
    }

    private void animateTruckView(final View v, int delay) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        v.setTranslationX(screenWidth * 0.5f);

        v.setVisibility(View.INVISIBLE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.setVisibility(View.VISIBLE);
                v.animate()
                        .translationX(0)
                        .setInterpolator(new DecelerateInterpolator())
                        .setDuration(400)
                        .start();
            }
        }, delay);
    }

    private void setupPreferences() {
        preferences = getPreferences(MODE_PRIVATE);

        String prefPostalCode = preferences.getString(PreferenceKeys.POSTAL_CODE, null);

        if (prefPostalCode != null) {
            //  Has previously successful postal code
            ((EditText) findViewById(R.id.text_input)).setText(prefPostalCode);
        }
    }

    private boolean validatePostalCode(String postalCode) {
        if (postalCode.length() < 4) {
            return false;
        }

        return true;
    }

    /**
     * Submits the inputted postal code and checks whether its deliverable.
     *
     * @param v
     */
    public void submitPostalCode(View v) {
        EditText inputField = (EditText) findViewById(R.id.text_input);
        String postalCode = inputField.getText().toString();

        if (!validatePostalCode(postalCode)) {
            new AlertDialog(this, R.string.invalid_pcode_header, R.string.invalid_pcode_message, new OneButtonAction(R.string.ok, null))
                    .show(getFragmentManager(), null);
            return;
        }

        lastSubmittedPostalCode = postalCode;

        BackendCom.request("out=check_deliverable&postal_code=" + postalCode, new byte[0], new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parsePostalCodeSubmission(response);
            }

            @Override
            public void onFailed() {
                onResponse("" + ResponseCodes.FAILED);
            }
        });
    }

    private void onPostalCodeResult(boolean deliverable) {
        if (deliverable) {
            preferences.edit().putString(PreferenceKeys.POSTAL_CODE, lastSubmittedPostalCode).apply();

            ShoppingCartActivity.setPostalCode(lastSubmittedPostalCode);
            //  To main
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            fetchCategories(mainIntent);
        } else {
            new AlertDialog(getString(R.string.undeliverable_header),
                    getString(R.string.undeliverable_message, lastSubmittedPostalCode),
                    new OneButtonAction(R.string.ok, null)).show(getFragmentManager(), null);
        }
    }

    /**
     * Fetches categories and puts the response in the main activity intent.
     */
    private void fetchCategories(final Intent intent) {
        BackendCom.request("out=categories", new byte[0], new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parseCategoriesResponse(response, intent);
            }

            @Override
            public void onFailed() {
                onBackendError();
            }
        });
    }

    private void parseCategoriesResponse(String response, Intent intent) {
        if (response.length() == 0) {
            //  No categories?
            onBackendError();
            return;
        }

        intent.putExtra("categories_response", response);

        //  Ready to start main activity
        startActivity(intent);
        finish();
    }

    /**
     * Fetches configuration.
     */
    private void fetchConfig() {
        BackendCom.request("out=config&names=min_order_cost,company_address,company_email,company_name,company_phone_num", new byte[0], new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parseConfigResponse(response);
            }

            @Override
            public void onFailed() {
                onBackendError();
            }
        });
    }

    private void parseConfigResponse(String response) {
        if (response.length() == 0) {
            //  No config?
            onBackendError();
            return;
        }

        try {

            JSONObject config = new JSONObject(response);
            ShoppingCartActivity.setMinimumCost(Double.parseDouble(config.getString("min_order_cost")));
            AboutAppFragment.setFromConfig(config);

        } catch (JSONException ex) {
            onBackendError();
            return;
        }

        animateFooter();
    }

    /**
     * Animates the footer once config has been fetched.
     */
    private void animateFooter() {
        View footer = findViewById(R.id.about_footer);

        footer.setVisibility(View.VISIBLE);
        footer.setAlpha(0);
        footer.setTranslationY(footer.getMeasuredHeight() * 0.5f);
        footer.animate()
                .translationY(0)
                .alpha(1)
                .start();
    }

    /**
     * Invoked when there was issues communicating with the backend.
     */
    private void onBackendError() {

    }

    private void parsePostalCodeSubmission(String response) {
        if (response.length() == 0) {
            response = "" + ResponseCodes.FAILED;
        }

        int responseCode = Integer.parseInt(response);

        switch (responseCode) {
            case ResponseCodes.POSITIVE:
                onPostalCodeResult(true);
                break;

            case ResponseCodes.NEGATIVE:
                onPostalCodeResult(false);
                break;

            case ResponseCodes.FAILED:
                //  TODO: Handle error
                break;
        }
    }

    public void showFeedbackDialog(View v) {
        AboutFooterHelper.getInstance().showFeedbackDialog(this);
    }

    public void showAboutApp(View v) {
        AboutFooterHelper.getInstance().showAboutApp(this);
    }
}
