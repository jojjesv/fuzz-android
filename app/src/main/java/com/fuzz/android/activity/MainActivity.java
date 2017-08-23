package com.fuzz.android.activity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.adapter.ArticlesAdapter;
import com.fuzz.android.adapter.CategoriesAdapter;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.format.Formatter;
import com.fuzz.android.fragment.ArticleInfoFragment;
import com.fuzz.android.util.StringUtils;
import com.fuzz.android.view.ArticleView;
import com.fuzz.android.view.ArticlesView;
import com.fuzz.android.view.CategoriesTutorial;
import com.fuzz.android.view.CategoriesView;
import com.fuzz.android.view.DefaultTypefaces;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ArticlesView.ArticleInfoListener, ArticlesView.ArticleDragListener, ShoppingCartActivity.ShoppingCartListener {

    private CategoriesAdapter.CategoryData[] categories;
    private ArrayList<Integer> selectedCategories;
    private boolean showingNoteViews;
    private int currentBackgroundColor;
    private int cartItemCount;
    private Interpolator cartCostRevealInterpolator;

    public MainActivity() {
        selectedCategories = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DefaultTypefaces.applyDefaultsToViews(this);

        fetchArticles();

        Intent sender = getIntent();
        parseCategories(sender.getStringExtra("categories_response"));
        setupLayout();

        ShoppingCartActivity.setShoppingCartListener(this);

        maybeStartTutorial();
    }

    private void setupLayout() {
        ArticlesView articles = (ArticlesView) findViewById(R.id.articles);
        articles.setItemsMovable(true);
        articles.setArticleDragListener(this);
        articles.setArticleInfoListener(this);

        CategoriesView categories = (CategoriesView) findViewById(R.id.categories);
        articles.setCategoriesView(categories);

        View root = findViewById(R.id.root);
        currentBackgroundColor = ((ColorDrawable) root.getBackground()).getColor();
    }

    private void maybeStartTutorial() {
        boolean start = false;
        if (start) {
            startTutorial();
        }
    }

    private void startTutorial() {
        new CategoriesTutorial(this);
    }

    private void updateCartCost() {
        double totalCost = ShoppingCartActivity.getTotalCost();

        TextView badge = (TextView) findViewById(R.id.cart_cost);

        if (totalCost == 0) {
            //  This won't occur while in MainActivity, no animation needed
            badge.setVisibility(View.GONE);
            cartItemCount = 0;
            return;
        }

        int itemCount = ShoppingCartActivity.getItemCount();

        String countStr = getString(R.string.cart_items, itemCount);
        String costStr = getString(R.string.cart_cost_appendage, Formatter.formatCurrency(totalCost));

        SpannableString spannable = new SpannableString(countStr + costStr);

        int costStart = countStr.length() - 1;

        ForegroundColorSpan countColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.white));
        spannable.setSpan(countColorSpan, 0, costStart + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ForegroundColorSpan costColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.white_translucent));
        spannable.setSpan(costColorSpan, costStart + 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);


        badge.setText(spannable);

        if (cartItemCount == 0) {
            //  Went from no items added
            Animation reveal = AnimationUtils.loadAnimation(this, R.anim.reveal_top_right);
            badge.startAnimation(reveal);
            badge.setVisibility(View.VISIBLE);
        }

        cartItemCount = itemCount;
    }

    private void parseCategories(String categoriesJson) {
        try {

            JSONArray categoriesArray = new JSONArray(categoriesJson);
            JSONObject category;
            int color;

            categories = new CategoriesAdapter.CategoryData[categoriesArray.length()];
            for (int i = 0, n = categoriesArray.length(); i < n; i++) {
                category = categoriesArray.getJSONObject(i);
                color = Color.parseColor("#" + category.getString("color"));
                categories[i] = new CategoriesAdapter.CategoryData(category.getInt("id"), category.getString("name"), color);
            }

            CategoriesAdapter adapter = new CategoriesAdapter(this, categories);
            ListView categoriesList = (ListView) findViewById(R.id.categories);

            categoriesList.setOnItemClickListener(createCategoryClickListener());
            categoriesList.setAdapter(adapter);

        } catch (JSONException ex) {
            //  TODO: Handle JSON error
        }
    }

    private AdapterView.OnItemClickListener createCategoryClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CategoriesAdapter.CategoryData data = ((CategoriesAdapter) adapterView.getAdapter()).getItem(i);
                changeCategory(data);
                view.findViewById(R.id.remove).setVisibility(data.enabled ? View.VISIBLE : View.GONE);
            }
        };
    }

    public void showCategories(@Nullable View v) {
        CategoriesView categoriesView = (CategoriesView) findViewById(R.id.categories);
        categoriesView.changeVisibility(false);
    }

    private void changeCategory(CategoriesAdapter.CategoryData categoryData) {
        boolean wasSelected = selectedCategories.contains(categoryData.id);
        categoryData.enabled = !wasSelected;

        if (!wasSelected) {
            //  Select category
            selectedCategories.add(categoryData.id);
        } else {
            //  Deselect category, cast to object not to remove by index
            selectedCategories.remove((Integer) categoryData.id);
        }

        fetchArticles();

        //changeBackgroundColor(categoryData.color);

        boolean isFrontpage = selectedCategories.isEmpty();

        TextView categoriesHeader = (TextView) findViewById(R.id.category_header);

        if (isFrontpage) {
            categoriesHeader.setText(R.string.popular_this_week);
        } else {
            ArrayList<String> categoryNames = new ArrayList<>();
            for (int i = 0, n = categories.length; i < n; i++) {
                if (categories[i].enabled) {
                    categoryNames.add(categories[i].name);
                }
            }
            categoriesHeader.setText(StringUtils.glue(this, categoryNames.toArray(new String[0])));
        }

        hideDragNoteViews();
    }

    private void changeBackgroundColor(final int newColor) {
        final View root = findViewById(R.id.root);
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            int fromR = Color.red(currentBackgroundColor);
            int fromG = Color.green(currentBackgroundColor);
            int fromB = Color.blue(currentBackgroundColor);
            int toR = Color.red(newColor);
            int toG = Color.green(newColor);
            int toB = Color.blue(newColor);

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();

                int r = (int) (fromR + (toR - fromR) * val);
                int g = (int) (fromG + (toG - fromG) * val);
                int b = (int) (fromB + (toB - fromB) * val);

                root.setBackgroundColor(Color.argb(255, r, g, b));
            }
        });
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        currentBackgroundColor = newColor;
    }

    public void fetchArticles() {
        StringBuilder queryString = new StringBuilder("out=articles");

        int selectedCategoriesSize = selectedCategories.size();

        if (selectedCategoriesSize > 0) {
            queryString.append("&categories=");

            //  Append selected categories
            for (int i = 0, n = selectedCategories.size() - 1; i <= n; i++) {

                queryString.append(selectedCategories.get(i)).append('.');

                if (i == n) {
                    //  Delete trailing delimiter
                    queryString.setLength(queryString.length() - 1);
                }
            }
        } else {
            //  Fetch popular articles
            queryString.append("&popular");
        }

        BackendCom.request(queryString.toString(), (byte[]) null, new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parseArticles(response);
            }

            @Override
            public void onFailed() {
                onResponse("" + ResponseCodes.FAILED);
            }
        });
    }

    @Override
    public void onStartedDrag(ArticleView view) {
        if (showingNoteViews) {
            hideDragNoteViews();
        }
    }

    private void parseArticles(String response) {
        try {

            JSONObject responseObj = new JSONObject(response);

            String baseImageUrl = responseObj.getString("base_image_url");
            JSONArray articles = responseObj.getJSONArray("articles");

            ArrayList<ArticlesAdapter.ArticleData> items = new ArrayList<>();
            ArticlesAdapter.ArticleData articleData;
            JSONObject article;

            //  Versions with different cost/quantity may occur
            String[] costs;
            String[] quantities;
            for (int i = 0, n = articles.length(); i < n; i++) {
                article = articles.getJSONObject(i);
                quantities = article.getString("quantities").split(",");
                costs = article.getString("costs").split(",");

                for (int j = 0, jn = quantities.length; j < jn; j++) {
                    articleData = new ArticlesAdapter.ArticleData(
                            article.getInt("id"),
                            Integer.parseInt(quantities[j]),
                            Double.parseDouble(costs[j]),
                            baseImageUrl + article.getString("image"),
                            article.getString("name"));

                    articleData.isNew = article.getBoolean("is_new");

                    items.add(articleData);
                }
            }

            ArticlesAdapter adapter = new ArticlesAdapter(items.toArray(new ArticlesAdapter.ArticleData[0]));
            adapter.setDarkMode(true);
            ((ArticlesView) findViewById(R.id.articles)).setAdapter(adapter);


        } catch (JSONException ex) {
            //  TODO: Handle JSON error
        }
    }

    @Override
    public void showArticleInfo(ArticlesAdapter.ArticleData article) {
        ArticleInfoFragment fragment = new ArticleInfoFragment();
        fragment.fetchArticleInfo(article);
        fragment.show(getFragmentManager(), null);
    }

    public void showShoppingCart(View v) {
        Intent i = new Intent(this, ShoppingCartActivity.class);
        startActivity(i);
    }

    /**
     * Shows the user where to drag an article.
     */
    private void showDragNoteViews() {
        //  Hide cart cost
        final View cartCost = findViewById(R.id.cart_cost);
        cartCost.animate().alpha(0).start();

        View cartNote = findViewById(R.id.shopping_cart_note);
        View infoNote = findViewById(R.id.article_info_note);

        cartNote.setVisibility(View.VISIBLE);
        infoNote.setVisibility(View.VISIBLE);

        Animation cartNoteRevealAnim = AnimationUtils.loadAnimation(this, R.anim.reveal_top_right);
        Animation infoNoteRevealAnim = AnimationUtils.loadAnimation(this, R.anim.reveal_top_left);

        cartNote.startAnimation(cartNoteRevealAnim);
        infoNote.startAnimation(infoNoteRevealAnim);

        showingNoteViews = true;
    }

    private void hideDragNoteViews() {
        //  Show cart cost
        final View cartCost = findViewById(R.id.cart_cost);
        cartCost.animate().alpha(1).start();

        final View cartNote = findViewById(R.id.shopping_cart_note);
        final View infoNote = findViewById(R.id.article_info_note);

        Animation cartNoteRevealAnim = AnimationUtils.loadAnimation(this, R.anim.hide_top_right);
        Animation infoNoteRevealAnim = AnimationUtils.loadAnimation(this, R.anim.hide_top_left);

        cartNote.startAnimation(cartNoteRevealAnim);
        infoNote.startAnimation(infoNoteRevealAnim);

        //  Finishes last
        cartNoteRevealAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cartNote.setVisibility(View.GONE);
                infoNote.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        showingNoteViews = false;
    }

    @Override
    public void onMissedDrag(ArticleView view) {
        if (!showingNoteViews) {
            showDragNoteViews();
        }
    }

    @Override
    public void onItemAdded(ArticlesAdapter.ArticleData item) {
        updateCartCost();
    }

    @Override
    public void onItemRemoved(ArticlesAdapter.ArticleData item) {
        updateCartCost();
    }
}
