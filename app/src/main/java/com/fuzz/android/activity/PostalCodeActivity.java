package com.fuzz.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.fuzz.android.R;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.view.DefaultTypefaces;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity for splash and postal code input.
 */
public class PostalCodeActivity extends Activity {
    private String lastSubmittedPostalCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DefaultTypefaces.setup(getResources());

        setContentView(R.layout.activity_postal_code);
        DefaultTypefaces.applyDefaultsToViews(this);
        //submitPostalCode(null);
    }

    /**
     * Submits the inputted postal code and checks whether its deliverable.
     *
     * @param v
     */
    public void submitPostalCode(View v) {
        EditText inputField = (EditText) findViewById(R.id.text_input);
        String postalCode = inputField.getText().toString();
        postalCode = "35052";

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
            ShoppingCartActivity.setPostalCode(lastSubmittedPostalCode);
            //  To main
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            fetchCategories(mainIntent);
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

        //  Config's next up
        fetchConfig(intent);
        //  Ready to start main activity
        startActivity(intent);
        finish();
    }

    /**
     * Fetches configuration.
     */
    private void fetchConfig(final Intent intent) {
        BackendCom.request("out=config&names=min_order_cost", new byte[0], new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parseConfigResponse(response, intent);
            }

            @Override
            public void onFailed() {
                onBackendError();
            }
        });
    }

    private void parseConfigResponse(String response, Intent intent) {
        if (response.length() == 0) {
            //  No config?
            onBackendError();
            return;
        }

        try {

            JSONObject result = new JSONObject(response);
            ShoppingCartActivity.setMinimumCost(Double.parseDouble(result.getString("min_order_cost")));

        } catch (JSONException ex){
            onBackendError();
            return;
        }

        //  Ready to start main activity
        startActivity(intent);
        finish();
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
}
