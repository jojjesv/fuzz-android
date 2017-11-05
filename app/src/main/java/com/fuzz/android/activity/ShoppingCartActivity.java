package com.fuzz.android.activity;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fuzz.android.R;
import com.fuzz.android.adapter.ArticlesAdapter;
import com.fuzz.android.animator.AnimatorAdapter;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.format.Formatter;
import com.fuzz.android.fragment.dialog.AlertDialog;
import com.fuzz.android.fragment.dialog.OneButtonAction;
import com.fuzz.android.helper.AboutFooterHelper;
import com.fuzz.android.listener.CardNumberFormatWatcher;
import com.fuzz.android.preferences.PreferenceKeys;
import com.fuzz.android.util.Encryption;
import com.fuzz.android.view.ArticleView;
import com.fuzz.android.view.ArticlesView;
import com.fuzz.android.view.DefaultTypefaces;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Activity for managing shopping cart and placing orders.
 */
public class ShoppingCartActivity extends Activity {
    /**
     * Map with an article and quantity as value.
     */
    private static final ArrayList<ArticlesAdapter.ArticleData> shoppingCart;
    private static String postalCode;
    private static ShoppingCartListener shoppingCartListener;
    /**
     * Minimum cost for ordering.
     */
    private static double minimumCost;
    /**
     * Delivery cost.
     */
    private static double deliveryCost;
    private static int lastOrderId;

    static {
        shoppingCart = new ArrayList<>();
    }

    private final int placeOrderTries = 3;
    private Interpolator removeItemInterpolator;
    private String paymentToken;
    private ArticlesAdapter articlesAdapter;
    private View oldPaymentInfoLayout;
    private int paymentMethodId;
    private View loadingView;
    private String labelRequiredAppendage;
    private ForegroundColorSpan labelRequiredSpan;
    private SharedPreferences preferences;
    /**
     * Remaining tries, as first may fail.
     */
    private int placeOrderTriesRemaining;

    public static int getLastOrderId() {
        return lastOrderId;
    }

    public static void setLastOrderId(int lastOrderId) {
        ShoppingCartActivity.lastOrderId = lastOrderId;
    }

    /**
     * @return A mapped shopping cart array where articles of same type are merged.
     */
    private static final ArticlesAdapter.ArticleData[] getMappedShoppingCart() {
        ArrayList<ArticlesAdapter.ArticleData> mapped = new ArrayList<>();
        ArticlesAdapter.ArticleData obj;
        for (ArticlesAdapter.ArticleData selected : shoppingCart) {
            if (mapped.size() == 0) {
                mapped.add(new ArticlesAdapter.ArticleData(selected));
            } else {
                //  Search for obj with same id
                for (int i = 0, n = mapped.size() - 1; i <= n; i++) {
                    obj = mapped.get(i);
                    if (obj.id == selected.id) {
                        obj.quantity += selected.quantity;
                        obj.cost += selected.cost;
                        break;
                    } else if (i == n) {
                        //  Last; doesn't exist
                        mapped.add(new ArticlesAdapter.ArticleData(selected));
                    }
                }
            }
        }

        return mapped.toArray(new ArticlesAdapter.ArticleData[0]);
    }

    public static double getDeliveryCost() {
        return deliveryCost;
    }

    public static void setDeliveryCost(double deliveryCost) {
        ShoppingCartActivity.deliveryCost = deliveryCost;
    }

    public static double getMinimumCost() {
        return minimumCost;
    }

    public static void setMinimumCost(double minimumCost) {
        ShoppingCartActivity.minimumCost = minimumCost;
    }

    public static ShoppingCartListener getShoppingCartListener() {
        return shoppingCartListener;
    }

    public static void setShoppingCartListener(ShoppingCartListener shoppingCartListener) {
        ShoppingCartActivity.shoppingCartListener = shoppingCartListener;
    }

    public static void setPostalCode(String postalCode) {
        ShoppingCartActivity.postalCode = postalCode;
    }

    public static void removeFromCart(ArticlesAdapter.ArticleData data) {
        for (int i = 0, n = shoppingCart.size(); i < n; i++) {
            //  Comparing id
            if (shoppingCart.get(i).id == data.id) {
                shoppingCart.remove(i);
                i--;
                n--;
                //  Don't break as there could be multiple occurrences
            }
        }

        if (shoppingCartListener != null) {
            shoppingCartListener.onItemRemoved(data);
        }
    }

    public static void addToCart(ArticlesAdapter.ArticleData data) {
        shoppingCart.add(data);
        if (shoppingCartListener != null) {
            shoppingCartListener.onItemAdded(data);
        }
    }

    private static String cartToString() {
        StringBuilder builder = new StringBuilder();

        for (ArticlesAdapter.ArticleData article : shoppingCart) {
            builder.append(article.id).append('x').append(article.quantity).append(',');
        }

        if (builder.length() > 0) {
            //  Delete trailing delimiter
            builder.setLength(builder.length() - 1);
        }

        return builder.toString();
    }

    public static double getCartCost() {
        double cartCost = 0;
        for (ArticlesAdapter.ArticleData article : shoppingCart) {
            cartCost += article.cost;
        }

        return cartCost;
    }

    public static double getTotalCost() {
        double totalCost = getCartCost();

        totalCost += deliveryCost;

        return totalCost;
    }

    public static int getItemCount() {
        return shoppingCart.size();
    }

    /**
     * Checks form data stored in preferences.
     */
    private void checkPreferences() {
        preferences = getPreferences(Context.MODE_PRIVATE);

        String address = preferences.getString(PreferenceKeys.ADDRESS, null);
        if (address != null) {
            ((EditText) findViewById(R.id.billing_address_input)).setText(address);
        }

        String floor = preferences.getString(PreferenceKeys.FLOOR, null);
        if (floor != null) {
            ((EditText) findViewById(R.id.floor_input)).setText(floor);
        }

        String doorCode = preferences.getString(PreferenceKeys.DOOR_CODE, null);
        if (doorCode != null) {
            ((EditText) findViewById(R.id.door_code_input)).setText(doorCode);
        }

        String fullName = preferences.getString(PreferenceKeys.FULL_NAME, null);
        if (fullName != null) {
            ((EditText) findViewById(R.id.full_name_input)).setText(fullName);
        }

        String phoneNum = preferences.getString(PreferenceKeys.PHONE_NUM, null);
        if (phoneNum != null) {
            ((EditText) findViewById(R.id.phone_input)).setText(phoneNum);
        }

        String cardDetails = preferences.getString(PreferenceKeys.CARD_NUMBER, null);
        if (cardDetails != null) {
            cardDetails = Encryption.decrypt(cardDetails);
            String[] fields = cardDetails.split(";");

            ((EditText) findViewById(R.id.card_number_input)).setText(fields[0]);

            String[] expiration = fields[1].split("/");
            for (int i = 0; i < expiration.length; i++) {
                if (expiration[i].length() == 1) {
                    //  Zero-fill
                    expiration[i] = "0" + expiration[i];
                }
            }
            View expireDateInput = findViewById(R.id.expire_date_input);

            ((EditText) expireDateInput.findViewById(R.id.expire_month_input)).setText(expiration[0]);
            ((EditText) expireDateInput.findViewById(R.id.expire_year_input)).setText(expiration[1]);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart);
        setupLayout();
        checkPreferences();
        updateSelectedArticlesView();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.shopping_cart_hide);
    }

    private void setupLayout() {
        View cardFields = findViewById(R.id.card_payment_group);

        View expireDate = findViewById(R.id.expire_date_input);
        final EditText monthExpireInput = (EditText) expireDate.findViewById(R.id.expire_month_input);
        final EditText yearExpireInput = (EditText) expireDate.findViewById(R.id.expire_year_input);

        expireDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                monthExpireInput.requestFocus();
            }
        });

        TextWatcher nextFieldOnMaxLen = new TextWatcher() {
            private int oldLen;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int len = charSequence.length();
                if (len == 2 && oldLen == 1) {
                    //  (User did input) Focus on year
                    yearExpireInput.requestFocus();
                }
                oldLen = len;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        monthExpireInput.addTextChangedListener(nextFieldOnMaxLen);

        EditText cardNumInput = (EditText) cardFields.findViewById(R.id.card_number_input);
        cardNumInput.addTextChangedListener(new CardNumberFormatWatcher());

        setupRadioGroups();

        ArticlesView articlesView = (ArticlesView) findViewById(R.id.articles);
        articlesView.setArticleListener(new ArticlesView.ArticleListener() {
            @Override
            public void onArticleRemoved(ArticlesAdapter.ArticleData data) {
                removeFromCart(data);

                if (shoppingCart.size() == 0) {
                    //  Empty cart, return to main
                    Toast t = Toast.makeText(ShoppingCartActivity.this, R.string.cart_now_empty, Toast.LENGTH_SHORT);

                    DrawableCompat.setTint(DrawableCompat.wrap(t.getView().getBackground()),
                            ResourcesCompat.getColor(getResources(), R.color.primary_dark, getTheme()));

                    t.show();

                    onBackPressed();
                    return;
                }
                updateTotalCost();
            }

            @Override
            public void onArticleClicked(ArticleView view) {

            }

            @Override
            public void onStartedDrag(ArticleView view) {

            }

            @Override
            public void onMissedDrag(ArticleView view) {

            }
        });

        loadingView = findViewById(R.id.loading);

        DefaultTypefaces.applyDefaultsToViews(this);

        labelRequiredAppendage = getString(R.string.label_required_appendage);
        labelRequiredSpan = new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.shopping_cart_light, getTheme()));

        appendRequiredToLabel((TextView) findViewById(R.id.label_card_number));
        appendRequiredToLabel((TextView) findViewById(R.id.label_card_expire));
        appendRequiredToLabel((TextView) findViewById(R.id.label_card_cvc));
        appendRequiredToLabel((TextView) findViewById(R.id.label_address));
        appendRequiredToLabel((TextView) findViewById(R.id.label_full_name));
        appendRequiredToLabel((TextView) findViewById(R.id.label_phone));
    }

    private void appendRequiredToLabel(TextView label) {
        CharSequence labelText = label.getText();

        SpannableString text = new SpannableString(labelText + labelRequiredAppendage);

        text.setSpan(labelRequiredSpan, labelText.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        label.setText(text);
    }

    private void setupRadioGroups() {
        RadioGroup paymentMethods = (RadioGroup) findViewById(R.id.payment_methods);
        RadioGroup.OnCheckedChangeListener checkedChangeListener = new RadioGroup.OnCheckedChangeListener() {
            int textSelectedColor;
            int drawableSelectedColor;
            int textNormalColor;
            int drawableNormalColor;
            private RadioButton previousRadioView;
            private int compoundDrawableIndex = 3;

            {
                Resources res = getResources();
                textSelectedColor = drawableSelectedColor = res.getColor(R.color.white);
                textNormalColor = res.getColor(R.color.payment_method_text_color);
                drawableNormalColor = res.getColor(R.color.payment_method_icon_tint);
            }

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
                paymentMethodId = id;

                RadioButton radioView = (RadioButton) radioGroup.findViewById(id);
                DrawableCompat.setTint(radioView.getCompoundDrawables()[compoundDrawableIndex], drawableSelectedColor);
                radioView.setTextColor(textSelectedColor);

                if (previousRadioView != null) {
                    //  Deselect previous
                    DrawableCompat.setTint(previousRadioView.getCompoundDrawables()[compoundDrawableIndex], drawableNormalColor);
                    previousRadioView.setTextColor(textNormalColor);
                }
                previousRadioView = radioView;

                switch (id) {
                    case R.id.payment_method_card:
                        changePaymentInfoGroup(findViewById(R.id.card_payment_group));
                        break;
                    case R.id.payment_method_cash:
                        changePaymentInfoGroup(findViewById(R.id.cash_payment_group));
                        break;
                    /*
                    case R.id.payment_method_swish:
                        changePaymentInfoGroup(findViewById(R.id.swish_payment_group));
                        break;
                        */
                }
            }
        };

        if (Build.VERSION.SDK_INT <= 19) {
            wrapRadioButtonCompoundDrawables(paymentMethods);
        }

        paymentMethods.setOnCheckedChangeListener(checkedChangeListener);
        checkedChangeListener.onCheckedChanged(paymentMethods, paymentMethods.getCheckedRadioButtonId());
    }

    private void wrapRadioButtonCompoundDrawables(RadioGroup group) {
        RadioButton[] buttons = new RadioButton[]{
                (RadioButton) group.findViewById(R.id.payment_method_card),
                (RadioButton) group.findViewById(R.id.payment_method_cash),
                //(RadioButton) group.findViewById(R.id.payment_method_swish),
        };

        Drawable compound;
        Resources res = getResources();
        int tint = res.getColor(R.color.payment_method_icon_tint);

        for (RadioButton btn : buttons) {
            compound = DrawableCompat.wrap(btn.getCompoundDrawables()[3]);

            btn.setCompoundDrawables(
                    null,
                    null,
                    null,
                    compound
            );

            DrawableCompat.setTint(compound, tint);
        }
    }

    public void openSwishApp(View v) {
        PackageManager pm = getPackageManager();
        PackageInfo targetPackage;
        try {
            targetPackage = pm.getPackageInfo("se.bankgirot.swish", 0);
        } catch (PackageManager.NameNotFoundException ex) {
            //  App not installed
            onSwishAppNotFound();
        }
    }

    private void onSwishAppNotFound() {

    }

    /**
     * Changes the currently visible payment info layout.
     *
     * @param newLayout New layout to show.
     */
    private void changePaymentInfoGroup(@Nullable View newLayout) {
        if (newLayout != null) {
            newLayout.setVisibility(View.VISIBLE);
        }

        if (oldPaymentInfoLayout != null) {
            //  Hide old layout
            oldPaymentInfoLayout.setVisibility(View.GONE);
        }

        oldPaymentInfoLayout = newLayout;
    }

    /**
     * Updates the view which shows the selected articles.
     */
    private void updateSelectedArticlesView() {
        ArticlesAdapter.ArticleData[] selectedArticles = getMappedShoppingCart();
        ArticlesAdapter adapter = new ArticlesAdapter(selectedArticles);
        adapter.setDarkMode(true);

        ArticlesView view = (ArticlesView) findViewById(R.id.articles);
        view.setRemovableOnClick(true);
        view.setItemAnimator(new DefaultItemAnimator());
        view.setAutoScrollFactor(160f);

        view.setAdapter(adapter);
        articlesAdapter = adapter;

        maybeAnimateScrollableArticles(view);

        updateTotalCost();
    }

    /**
     * Indicates that the articles view is scrollable.
     */
    private void maybeAnimateScrollableArticles(final ArticlesView articlesView) {
        if (!preferences.getBoolean(PreferenceKeys.SHOW_CART_SCROLL_HINT, true)) {
            return;
        }

        preferences.edit().putBoolean(PreferenceKeys.SHOW_CART_SCROLL_HINT, false).apply();

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LinearLayoutManager layoutManager = (LinearLayoutManager) articlesView.getLayoutManager();
                if (layoutManager.findLastCompletelyVisibleItemPosition() < articlesView.getAdapter().getItemCount()) {
                    //  Scrollable
                    animateScrollableArticles(articlesView);
                }
            }
        }, 1000);
    }

    private void animateScrollableArticles(final ArticlesView articlesView) {
        articlesView.smoothScrollToPosition(
                ((LinearLayoutManager) articlesView.getLayoutManager()).findLastVisibleItemPosition() + 1
        );
    }

    private void updateTotalCost() {
        //  Update total cost label
        double cost = getCartCost();

        ForegroundColorSpan costAmountSpan = new ForegroundColorSpan(getResources().getColor(R.color.white_translucent));

        String prefix;
        String suffix;


        //  Articles cost view
        TextView articlesCostView = (TextView) findViewById(R.id.cost_articles);
        prefix = getString(R.string.cost_add_articles);
        suffix = getString(R.string.cost_add_appendage, Formatter.formatCurrency(cost));

        SpannableString articlesCostSpannable = new SpannableString(prefix + suffix);
        articlesCostSpannable.setSpan(costAmountSpan, prefix.length(), articlesCostSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        articlesCostView.setText(articlesCostSpannable);


        TextView costBelowMinView = (TextView) findViewById(R.id.cost_below_min);

        if (cost < minimumCost) {
            //  Adds delta cost
            double deltaCost = minimumCost - cost;
            costBelowMinView.setVisibility(View.VISIBLE);

            prefix = getString(R.string.cost_add_below_min, Formatter.formatCurrency(minimumCost));
            suffix = getString(R.string.cost_add_appendage, Formatter.formatCurrency(deltaCost));

            SpannableString belowMinSpannable = new SpannableString(prefix + suffix);
            belowMinSpannable.setSpan(costAmountSpan, prefix.length(), belowMinSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            costBelowMinView.setText(belowMinSpannable);

            cost = minimumCost;

        } else {
            costBelowMinView.setVisibility(View.GONE);
        }


        //  Delivery cost view
        TextView deliveryCostView = (TextView) findViewById(R.id.delivery_cost);
        prefix = getString(R.string.cost_add_delivery);
        suffix = getString(R.string.cost_add_appendage, Formatter.formatCurrency(deliveryCost));

        SpannableString deliveryCostSpannable = new SpannableString(prefix + suffix);
        deliveryCostSpannable.setSpan(costAmountSpan, prefix.length(), deliveryCostSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        deliveryCostView.setText(deliveryCostSpannable);

        cost += deliveryCost;

        //  Total cost view
        TextView totalCostView = (TextView) findViewById(R.id.total_cost);
        prefix = getString(R.string.total_cost);
        suffix = getString(R.string.cost_add_appendage, Formatter.formatCurrency(Math.max(cost, minimumCost)));

        SpannableString totalCostSpannable = new SpannableString(prefix + suffix);
        totalCostSpannable.setSpan(costAmountSpan, prefix.length(), totalCostSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        totalCostView.setText(totalCostSpannable);
    }

    private void requestPaymentToken() {
        BackendCom.request("out=payment_token", (byte[]) null, new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parsePaymentToken(response);
            }

            @Override
            public void onFailed() {

            }
        });
    }

    private void parsePaymentToken(String response) {
        paymentToken = response;
    }

    public void beginPlacingOrder(@Nullable View v) {
        String invalidFormMessage = validateForm();
        if (invalidFormMessage != null) {
            //  Has error message
            showErrorMessage(invalidFormMessage);
            return;
        }

        showLoading();

        placeOrder();
    }

    /**
     * Validates the form.
     *
     * @return Error message, or null if form is valid.
     */
    private String validateForm() {
        //  Validate required fields here
        EditText billingAddress = (EditText) findViewById(R.id.billing_address_input);

        if (billingAddress.getText().length() < 3) {
            onFieldInvalid(billingAddress);
            return getString(R.string.invalid_billing_address);
        }

        EditText orderer = (EditText) findViewById(R.id.full_name_input);

        if (orderer.getText().length() < 2) {
            onFieldInvalid(orderer);
            return getString(R.string.invalid_orderer);
        }

        EditText phone = (EditText) findViewById(R.id.phone_input);
        int phoneLen = phone.getText().length();

        if (phoneLen != 10 && phoneLen != 12) {
            onFieldInvalid(phone);
            return getString(R.string.invalid_phone);
        }

        switch (paymentMethodId) {
            case R.id.payment_method_card:

                EditText cardNumber = (EditText) findViewById(R.id.card_number_input);
                if (cardNumber.getText().length() != 4 * 4 + 3) {
                    onFieldInvalid(cardNumber);
                    return getString(R.string.invalid_card_number);
                }

                View expireDateInput = findViewById(R.id.expire_date_input);
                Editable expireMonth =
                        ((EditText) expireDateInput.findViewById(R.id.expire_month_input)).getText();
                Editable expireYear =
                        ((EditText) expireDateInput.findViewById(R.id.expire_year_input)).getText();

                if (expireMonth.length() != 2 || expireYear.length() != 2) {
                    onFieldInvalid(expireDateInput);
                    return getString(R.string.invalid_card_number);
                }

                EditText cvc = (EditText) findViewById(R.id.cvc_input);
                if (cvc.getText().length() != 3) {
                    onFieldInvalid(cvc);
                    return getString(R.string.invalid_card_cvc);
                }

                break;
        }

        return null;
    }

    /**
     * Called when a form field has invalid value upon submission.
     *
     * @param field
     */
    private void onFieldInvalid(View field) {
        field.requestFocus();
    }

    public void requestPlaceOrder(String postData) {
        BackendCom.request("action=place_order", postData, new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parsePlaceOrderResponse(response);
            }

            @Override
            public void onFailed() {
                onResponse("" + ResponseCodes.FAILED);
            }
        });
    }

    private void showErrorMessage(String message) {
        new AlertDialog()
                .setHeader(getString(R.string.invalid_form_header))
                .setText(message)
                .setActions(new OneButtonAction(R.string.ok, null))
                .show(getFragmentManager(), null);
    }

    /**
     * Confirms and places the order.
     */
    public void placeOrder() {
        String billingAddress = ((EditText) findViewById(R.id.billing_address_input)).getText().toString();
        String floor = ((EditText) findViewById(R.id.floor_input)).getText().toString();
        String doorCode = ((EditText) findViewById(R.id.door_code_input)).getText().toString();
        String message = ((EditText) findViewById(R.id.message_input)).getText().toString();
        String fullName = ((EditText) findViewById(R.id.full_name_input)).getText().toString();
        String phoneNum = ((EditText) findViewById(R.id.phone_input)).getText().toString();

        //  Store form data
        preferences.edit().
                putString(PreferenceKeys.ADDRESS, billingAddress).
                putString(PreferenceKeys.FLOOR, floor).
                putString(PreferenceKeys.DOOR_CODE, doorCode).
                putString(PreferenceKeys.FULL_NAME, fullName).
                putString(PreferenceKeys.PHONE_NUM, phoneNum).
                apply();

        RadioGroup paymentMethods = (RadioGroup) findViewById(R.id.payment_methods);
        int paymentMethodId = paymentMethods.getCheckedRadioButtonId();
        String paymentMethod = getPaymentMethodFromId(paymentMethodId);

        String paymentInfo = getPaymentInfo();
        String items = cartToString();

        StringBuilder postBuilder = new StringBuilder();
        postBuilder.append("billing_address=").append(BackendCom.encode(billingAddress));
        postBuilder.append("&payment_method=").append(paymentMethod);
        postBuilder.append("&payment_info=").append(BackendCom.encode(paymentInfo));
        postBuilder.append("&orderer=").append(BackendCom.encode(fullName));
        postBuilder.append("&cart_items=").append(items);
        postBuilder.append("&postal_code=").append(postalCode);
        postBuilder.append("&phone_num=").append(BackendCom.encode(phoneNum));

        if (floor.length() > 0) {
            postBuilder.append("&floor=").append(floor);
        }
        if (doorCode.length() > 0) {
            postBuilder.append("&door_code=").append(doorCode);
        }

        if (message.length() > 0) {
            postBuilder.append("&message=").append(BackendCom.encode(message));
        }

        placeOrderTriesRemaining = placeOrderTries;

        if (paymentMethod.contentEquals("card")) {
            submitCardPaymentWithViews(postBuilder.toString());
        } else {
            requestPlaceOrder(postBuilder.toString());
        }
    }

    /**
     * Calls submitCardPayment with view texts.
     */
    private void submitCardPaymentWithViews(String orderPostData) {
        View cardGroup = findViewById(R.id.card_payment_group);

        EditText cardNumberInput = (EditText) cardGroup.findViewById(R.id.card_number_input);
        View expireInput = cardGroup.findViewById(R.id.expire_date_input);
        EditText cvcInput = (EditText) cardGroup.findViewById(R.id.cvc_input);

        int expireMonth = Integer.parseInt(
                ((EditText) expireInput.findViewById(R.id.expire_month_input)).getText().toString()
        );
        int expireYear = Integer.parseInt(
                ((EditText) expireInput.findViewById(R.id.expire_year_input)).getText().toString()
        );

        submitCardPayment(
                cardNumberInput.getText().toString(),
                expireMonth,
                expireYear,
                cvcInput.getText().toString(),
                orderPostData
        );
    }

    private void submitCardPayment(String cardNumber, int expireMonth, int expireYear, String cardCvc, final String orderPostData) {
        Card card = new Card(cardNumber, expireMonth, expireYear, cardCvc);

        if (!card.validateNumber() || !card.validateCVC() || !card.validateExpMonth() || !card.validateExpYear()) {
            onInvalidCard();
            return;
        }

        final String cardDetails = cardNumber + ';' + expireMonth + '/' + expireYear;

        Stripe stripe = new Stripe(this, "pk_test_pUCEDa47lAxtjphvdwdVT7j2");
        stripe.createToken(card, new TokenCallback() {
            @Override
            public void onError(Exception error) {
                onPaymentError(error);
            }

            @Override
            public void onSuccess(Token token) {
                //  Store card data
                preferences.edit()
                        .putString(PreferenceKeys.CARD_NUMBER, Encryption.encrypt(cardDetails))
                        .apply();

                processCardPayment(token, orderPostData);
            }
        });
    }

    private void showLoading() {
        View view = loadingView;
        view.setAlpha(0);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1)
                .setDuration(500)
                .start();
    }

    private void hideLoading() {
        View view = loadingView;
        view.animate()
                .alpha(0)
                .setListener(new AnimatorAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        loadingView.setVisibility(View.GONE);
                    }
                })
                .setDuration(100)
                .start();
        view.setVisibility(View.VISIBLE);
    }

    /**
     * Submits card token and processes payment.
     *
     * @param token
     */
    private void processCardPayment(Token token, String orderData) {
        StringBuilder postBuilder = new StringBuilder("token_id=");
        postBuilder.append(token.getId());
        postBuilder.append('&').append(orderData);
        BackendCom.request("action=process_card_payment", postBuilder.toString(), new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                //  Did place order
                parsePlaceOrderResponse(response);
            }

            @Override
            public void onFailed() {
                onPaymentError(null);
            }
        });
    }

    private void onInvalidCard() {
        showErrorMessage(getString(R.string.invalid_card));
        hideLoading();
    }

    private void onPaymentError(Exception ex) {
        Log.e("Payment", "Payment error", ex);
        showErrorMessage(getString(R.string.generic_card_error));
        hideLoading();
    }

    private void parsePlaceOrderResponse(String response) {
        try {

            JSONObject responseObj = new JSONObject(response);
            int status = responseObj.getInt("status");
            if (status == ResponseCodes.SUCCESS) {
                onOrderPlaced(responseObj.getInt("order_id"));
            } else if (status == ResponseCodes.FAILED) {
                if (placeOrderTriesRemaining-- > 0) {
                    //  Retry
                    placeOrder();
                } else {
                    onPlaceOrderFailed(responseObj.has("message") ? responseObj.getString("message") : null);
                }
            }

        } catch (JSONException ex) {
            //  TODO: Handle JSON errors
        }
    }

    private void onPlaceOrderFailed(String message) {
        Log.e(getClass().getSimpleName(), "Order placed failed. Msg: " + message);

        if (message != null) {
            new AlertDialog()
                    .setHeader(getString(R.string.something_wrong))
                    .setText(message)
                    .setActions(new OneButtonAction(R.string.ok, null))
                    .show(getFragmentManager(), null);
        }

        hideLoading();
    }

    /**
     * Starts a service which notifies the user when ETA for the order changes.
     *
     * @param orderId
     */
    private void onOrderPlaced(int orderId) {
        //  Clear cart
        ShoppingCartActivity.shoppingCart.clear();

        Intent i = new Intent(this, PostOrderActivity.class);
        setLastOrderId(orderId);
        i.putExtra("order_id", orderId);
        startActivity(i);
        finish();
    }

    private String getPaymentInfo() {
        return "+467251129";
    }

    private String getPaymentMethodFromId(int id) {
        switch (id) {
            case R.id.payment_method_card:
                return "card";
            default:
            case R.id.payment_method_cash:
                return "cash";
            /* case R.id.payment_method_swish:
                return "swish";
                */
        }
    }

    public void showFeedbackDialog(View v) {
        AboutFooterHelper.getInstance().showFeedbackDialog(this);
    }

    public void showAboutApp(View v) {
        AboutFooterHelper.getInstance().showAboutApp(this);
    }

    public interface ShoppingCartListener {
        void onItemAdded(ArticlesAdapter.ArticleData item);

        void onItemRemoved(ArticlesAdapter.ArticleData item);
    }
}
