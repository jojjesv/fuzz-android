package com.fuzz.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.adapter.ArticlesAdapter;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.format.Formatter;
import com.fuzz.android.fragment.dialog.AlertDialog;
import com.fuzz.android.fragment.dialog.OneButtonAction;
import com.fuzz.android.helper.AboutFooterHelper;
import com.fuzz.android.listener.CardNumberFormatWatcher;
import com.fuzz.android.listener.MonthYearFormatWatcher;
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

    static {
        shoppingCart = new ArrayList<>();
    }

    private Interpolator removeItemInterpolator;
    private String paymentToken;
    private ArticlesAdapter articlesAdapter;
    private View oldPaymentInfoLayout;
    private int paymentInfoGroupId;

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
        shoppingCart.remove(data);
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

    public static double getTotalCost() {
        double totalCost = 0;
        for (ArticlesAdapter.ArticleData article : shoppingCart) {
            totalCost += article.cost;
        }

        return totalCost;
    }

    public static int getItemCount() {
        return shoppingCart.size();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart);
        setupLayout();
        updateSelectedArticlesView();
    }

    private void setupLayout() {
        View cardFields = findViewById(R.id.card_payment_group);

        EditText cardNumInput = (EditText) cardFields.findViewById(R.id.card_number_input);
        cardNumInput.addTextChangedListener(new CardNumberFormatWatcher());

        EditText cardExpireInput = (EditText) cardFields.findViewById(R.id.expire_date_input);
        cardExpireInput.addTextChangedListener(new MonthYearFormatWatcher(cardExpireInput));

        setupRadioGroups();

        DefaultTypefaces.applyDefaultsToViews(this);
    }

    private void setupRadioGroups() {
        RadioGroup paymentMethods = (RadioGroup) findViewById(R.id.payment_methods);
        paymentMethods.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
                paymentInfoGroupId = id;

                switch (id) {
                    case R.id.payment_method_card:
                        changePaymentInfoGroup(findViewById(R.id.card_payment_group));
                        break;
                    case R.id.payment_method_cash:
                        changePaymentInfoGroup(findViewById(R.id.cash_payment_group));
                        break;
                    case R.id.payment_method_swish:
                        changePaymentInfoGroup(findViewById(R.id.swish_payment_group));
                        break;
                }
            }
        });
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
        ArticlesAdapter.ArticleData[] selectedArticles = shoppingCart.toArray(new ArticlesAdapter.ArticleData[0]);
        ArticlesAdapter adapter = new ArticlesAdapter(selectedArticles);
        adapter.setItemsOnClickListener(createArticleClickListener());
        ArticlesView view = (ArticlesView) findViewById(R.id.selected_articles);

        view.setAdapter(adapter);
        articlesAdapter = adapter;

        updateTotalCost();
    }

    private void updateTotalCost() {
        //  Update total cost label
        double totalCost = getTotalCost();

        ForegroundColorSpan costAmountSpan = new ForegroundColorSpan(getResources().getColor(R.color.white_translucent));

        String prefix;
        String suffix;

        TextView costBelowMinView = (TextView) findViewById(R.id.cost_below_min);

        if (totalCost < minimumCost) {
            //  Adds delta cost
            double deltaCost = minimumCost - totalCost;
            costBelowMinView.setVisibility(View.VISIBLE);

            prefix = getString(R.string.cost_add_below_min, Formatter.formatCurrency(minimumCost));
            suffix = getString(R.string.cost_add_appendage, Formatter.formatCurrency(deltaCost));

            SpannableString belowMinSpannable = new SpannableString(prefix + suffix);
            belowMinSpannable.setSpan(costAmountSpan, prefix.length(), belowMinSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            costBelowMinView.setText(belowMinSpannable);
        } else {
            costBelowMinView.setVisibility(View.GONE);
        }

        //  Total cost view
        TextView totalCostView = (TextView) findViewById(R.id.total_cost);
        prefix = getString(R.string.total_cost);
        suffix = getString(R.string.cost_add_appendage, Formatter.formatCurrency(Math.max(totalCost, minimumCost)));

        SpannableString totalCostSpannable = new SpannableString(prefix + suffix);
        totalCostSpannable.setSpan(costAmountSpan, prefix.length(), totalCostSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        totalCostView.setText(totalCostSpannable);

        //  Articles cost view
        TextView articlesCostView = (TextView) findViewById(R.id.cost_articles);
        prefix = getString(R.string.cost_add_articles);
        suffix = getString(R.string.cost_add_appendage, Formatter.formatCurrency(totalCost));

        SpannableString articlesCostSpannable = new SpannableString(prefix + suffix);
        articlesCostSpannable.setSpan(costAmountSpan, prefix.length(), articlesCostSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        articlesCostView.setText(articlesCostSpannable);
    }

    /**
     * @return Click listener which will remove an item from the cart upon click.
     */
    private View.OnClickListener createArticleClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeItem((ArticlesAdapter.ArticleData) view.getTag(), view);
            }
        };
    }

    private void removeItem(final ArticlesAdapter.ArticleData article, final View v) {
        removeFromCart(article);

        ArrayList<ArticlesAdapter.ArticleData> items = articlesAdapter.getItems();
        for (int i = 0, n = items.size(); i < n; i++) {
            if (items.get(i).id == article.id) {
                //  Remove this item
                items.remove(i);
                articlesAdapter.notifyItemRemoved(i);
                break;
            }
        }

        updateTotalCost();
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
        if (!validateForm()) {
            return;
        }

        placeOrder();
    }

    private boolean validateForm(){
        EditText billingAddress = (EditText) findViewById(R.id.billing_address_input);

        if (billingAddress.getText().length() < 3) {
            onFieldInvalid(billingAddress);
            return false;
        }

        switch (paymentInfoGroupId) {
            case R.id.card_payment_group:

                EditText cardNumber = (EditText)findViewById(R.id.card_number_input);
                if (cardNumber.getText().length() != 4 * 4 + 3) {
                    onFieldInvalid(cardNumber);
                    return false;
                }

                EditText expire = (EditText)findViewById(R.id.expire_date_input);
                Editable expireText = expire.getText();
                if (expireText.length() != 5 || !expireText.toString().matches("\\d+\\/\\d+")) {
                    onFieldInvalid(billingAddress);
                    return false;
                }

                EditText cvc = (EditText)findViewById(R.id.cvc_input);
                if (cvc.getText().length() != 3) {
                    onFieldInvalid(cvc);
                    return false;
                }

                break;
        }

        return true;
    }

    /**
     * Called when a form field has invalid value upon submission.
     * @param field
     */
    private void onFieldInvalid(View field){

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

    /**
     * Confirms and places the order.
     */
    public void placeOrder() {
        String billingAddress = ((EditText) findViewById(R.id.billing_address_input)).getText().toString();

        String houseNumber = ((EditText) findViewById(R.id.house_number_input)).getText().toString();
        String floor = ((EditText) findViewById(R.id.floor_input)).getText().toString();
        String doorCode = ((EditText) findViewById(R.id.door_code_input)).getText().toString();

        String message = ((EditText) findViewById(R.id.message_input)).getText().toString();

        RadioGroup paymentMethods = (RadioGroup) findViewById(R.id.payment_methods);
        int paymentMethodId = paymentMethods.getCheckedRadioButtonId();
        String paymentMethod = getPaymentMethodFromId(paymentMethodId);

        String paymentInfo = getPaymentInfo();
        String items = cartToString();

        StringBuilder postBuilder = new StringBuilder();
        postBuilder.append("billing_address=").append(BackendCom.encode(billingAddress));
        postBuilder.append("&payment_method=").append(paymentMethod);
        postBuilder.append("&payment_info=").append(BackendCom.encode(paymentInfo));
        postBuilder.append("&cart_items=").append(items);
        postBuilder.append("&postal_code=").append(postalCode);

        if (houseNumber.length() > 0) {
            postBuilder.append("&house_number=").append(houseNumber);
        }
        if (floor.length() > 0) {
            postBuilder.append("&floor=").append(floor);
        }
        if (doorCode.length() > 0) {
            postBuilder.append("&door_code=").append(doorCode);
        }

        if (message.length() > 0) {
            postBuilder.append("&message=").append(BackendCom.encode(message));
        }

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
        EditText expireInput = (EditText) cardGroup.findViewById(R.id.expire_date_input);
        EditText cvcInput = (EditText) cardGroup.findViewById(R.id.cvc_input);

        String expireString = expireInput.getText().toString();
        int expireDelimiterIndex = expireString.indexOf('/');

        int expireMonth = Integer.parseInt(expireString.substring(0, expireDelimiterIndex));
        int expireYear = Integer.parseInt(expireString.substring(expireDelimiterIndex + 1));

        submitCardPayment(cardNumberInput.getText().toString(), expireMonth, expireYear, cvcInput.getText().toString(), orderPostData);
    }

    private void submitCardPayment(String cardNumber, int expireMonth, int expireYear, String cardCvc, final String orderPostData) {
        Card card = new Card(cardNumber, expireMonth, expireYear, cardCvc);

        if (!card.validateNumber() || !card.validateCVC() || !card.validateExpMonth() || !card.validateExpYear()) {
            onInvalidCard();
        }

        Stripe stripe = new Stripe(this, "pk_test_pUCEDa47lAxtjphvdwdVT7j2");
        stripe.createToken(card, new TokenCallback() {
            @Override
            public void onError(Exception error) {
                onPaymentError(error);
            }

            @Override
            public void onSuccess(Token token) {
                processCardPayment(token, orderPostData);
            }
        });
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

    }

    private void onPaymentError(Exception ex) {

    }

    private void parsePlaceOrderResponse(String response) {
        try {

            JSONObject responseObj = new JSONObject(response);
            int status = responseObj.getInt("status");
            if (status == ResponseCodes.SUCCESS) {
                onOrderPlaced(responseObj.getInt("order_id"));
            } else if (status == ResponseCodes.NEGATIVE) {
                onPlaceOrderFailed(responseObj.has("message") ? responseObj.getString("message") : null);
            }

        } catch (JSONException ex) {
            //  TODO: Handle JSON errors
        }
    }

    private void onPlaceOrderFailed(String message) {
        Log.e(getClass().getSimpleName(), "Order placed failed. Msg: " + message);

        if (message != null) {
            new AlertDialog(getString(R.string.something_wrong), message, new OneButtonAction(R.string.ok, null));
        }
    }

    /**
     * Starts a service which notifies the user when ETA for the order changes.
     *
     * @param orderId
     */
    private void onOrderPlaced(int orderId) {
        Intent i = new Intent(this, PostOrderActivity.class);
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
            case R.id.payment_method_swish:
                return "swish";
        }
    }

    public interface ShoppingCartListener {
        public void onItemAdded(ArticlesAdapter.ArticleData item);

        public void onItemRemoved(ArticlesAdapter.ArticleData item);
    }

    public void showFeedbackDialog(View v) {
        AboutFooterHelper.getInstance().showFeedbackDialog(this);
    }

    public void showAboutApp(View v) {

    }
}
