package com.fuzz.android.activity;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.adapter.ArticlesAdapter;
import com.fuzz.android.adapter.CategoriesAdapter;
import com.fuzz.android.animator.AnimatorAdapter;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.format.Formatter;
import com.fuzz.android.fragment.ArticleInfoFragment;
import com.fuzz.android.fragment.dialog.AlertDialog;
import com.fuzz.android.fragment.dialog.OneButtonAction;
import com.fuzz.android.listener.MainArticlesScrollListener;
import com.fuzz.android.net.Caches;
import com.fuzz.android.preferences.PreferenceKeys;
import com.fuzz.android.service.EtaChangeNotifier;
import com.fuzz.android.util.StringUtils;
import com.fuzz.android.view.ArticleView;
import com.fuzz.android.view.ArticlesContainerView;
import com.fuzz.android.view.ArticlesView;
import com.fuzz.android.view.CategoriesTutorial;
import com.fuzz.android.view.CategoriesView;
import com.fuzz.android.view.DefaultTypefaces;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity implements ArticlesView.ArticleInfoListener, ArticlesView.ArticleListener, ShoppingCartActivity.ShoppingCartListener,
        MainArticlesScrollListener.FetchNextPageListener {

    private static JSONObject config;

    /**
     * Parses configuration.
     *
     * @param config Configuration JSON
     */
    public static void setConfig(JSONObject config) {
        MainActivity.config = config;
    }

    private CategoriesAdapter.CategoryData[] categories;
    private ArrayList<Integer> selectedCategories;
    private boolean showingNoteViews;
    private int currentBackgroundColor;
    private int cartItemCount;
    private Interpolator cartCostRevealInterpolator;
    private int dragNotesShowCount;
    private android.os.Handler handler;
    private SharedPreferences preferences;
    private int currentArticlePage;
    private boolean fetchedLastArticlePage;
    private int articlesPerPage;
    private boolean fetchingArticles;
    private View[] backgroundItems;
    private float[] backgroundItemParallax;
    private View articlesFetchIndicatorView;
    private View actionBarOverlay;
    private ImageView actionBarLogo;
    private CategoriesView categoriesView;
    private boolean runningOrderEtaService;

    public MainActivity() {
        selectedCategories = new ArrayList<>();
        handler = new Handler();
    }

    @Override
    public void fetchNextPage() {
        if (canFetchArticles()) {
            currentArticlePage++;
            fetchArticles();
        }
    }

    @Override
    public void onBackPressed() {
        if (categoriesView.isVisible()) {
            categoriesView.changeVisibility(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DefaultTypefaces.applyDefaultsToViews(this);

        preferences = getPreferences(Context.MODE_PRIVATE);

        Intent sender = getIntent();
        parseCategories(sender.getStringExtra("categories_response"));
        setupLayout();

        ShoppingCartActivity.setShoppingCartListener(this);

        fetchArticles();

        if (!parseConfig()) {
            maybeShowTutorial();
        }
    }

    private void setupLayout() {
        categoriesView = (CategoriesView) findViewById(R.id.categories);
        actionBarOverlay = findViewById(R.id.action_bar_overlay);
        actionBarLogo = (ImageView) findViewById(R.id.logo);
        articlesFetchIndicatorView = findViewById(R.id.articles_fetch_indicator);

        ArticlesView articles = (ArticlesView) findViewById(R.id.articles);
        articles.setItemsMovable(true);
        articles.setArticleInfoListener(this);
        articles.setArticleListener(this);
        articles.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                scrollBackgroundBy(-dy * 0.2f);
            }
        });

        //  Enables fetching next page once reached bottom
        articles.setScrollUpdateListener(new MainArticlesScrollListener(this));

        CategoriesView categories = (CategoriesView) findViewById(R.id.categories);
        articles.setCategoriesView(categories);
        categories.setTransitionListener(createCategoriesTransitionListener());

        Resources res = getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        int articlesPerRow = res.getInteger(R.integer.articles_per_row);
        int articleHeight = res.getDimensionPixelSize(R.dimen.article_item_height);
        int rowsPerPage = (int) Math.ceil((float) displayMetrics.heightPixels / articleHeight) + 2; //  Consider action bar

        articlesPerPage = rowsPerPage * articlesPerRow;

        ArticlesContainerView articlesContainer = (ArticlesContainerView) findViewById(R.id.articles_container);

        categories.setContainer(articlesContainer);
        articles.setContainer(articlesContainer);

        setupLayoutBackground();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cartItemCount != ShoppingCartActivity.getItemCount()) {
            updateCartCost();
        }

        if (!runningOrderEtaService && EtaChangeNotifier.isActive()) {
            //  Run service
            Intent serviceIntent = new Intent(this, EtaChangeNotifier.class);
            serviceIntent.putExtra("order_id", ShoppingCartActivity.getLastOrderId());

            startService(serviceIntent);

            runningOrderEtaService = true;
        }
    }

    /**
     * Parses the config and maybe shows a dialog. Returns true if so.
     */
    private boolean parseConfig() {
        try {

            Calendar calendar = GregorianCalendar.getInstance();
            String daysOpen = config.getString("open_days");

            int day = calendar.get(Calendar.DAY_OF_WEEK);
            char dayChar = Character.forDigit(day, 10);
            for (int i = 0, n = daysOpen.length() - 1; i <= n; i++) {
                if (daysOpen.charAt(i) == dayChar) {
                    //  Acceptable
                    break;
                }

                if (i == n) {
                    //  TODO: Mentions the next available day for delivery
                    //  Not deliverable at this day of week
                    onUndeliverable(day, null);
                    return true;
                }
            }

            //  Check time
            String openTime = config.getString("open_time");
            String closeTime = config.getString("closing_time");

            String[] openTimeFields = openTime.split(":");
            String[] closeTimeFields = closeTime.split(":");

            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);

            boolean currentBeforeOpenTime = currentMinute + 60 * currentHour <
                    Integer.parseInt(openTimeFields[0]) + 60 * Integer.parseInt(openTimeFields[1]);

            boolean currentAfterCloseTime = currentMinute + 60 * currentHour >
                    Integer.parseInt(closeTimeFields[1]) + 60 * Integer.parseInt(closeTimeFields[0]);

            if (currentBeforeOpenTime || currentAfterCloseTime) {
                //  Before opening or after closing
                onUndeliverable(0, openTime);
                return true;
            }

        } catch (JSONException ex) {

        }

        return false;
    }

    /**
     * Called if delivery is current unavailable.
     *
     * @param dayOfWeek Optional (0 if none), if a day of week is undeliverable.
     * @param openTime  Optional (nullable), if undeliverable until such open time (HH:mm).
     */
    private void onUndeliverable(int dayOfWeek, @Nullable String openTime) {
        boolean isDayOfWeek = dayOfWeek > 0;

        String dayOfWeekString = isDayOfWeek ? getResources().getStringArray(R.array.days_of_week)[dayOfWeek - 1]
                : null;
        new AlertDialog()
                .setHeader(getString(R.string.undeliverable_datetime_header))
                .setText(
                        getString(R.string.undeliverable_datetime,
                                getString(isDayOfWeek ? R.string.undeliverable_datetime_day : R.string.undeliverable_datetime_time,
                                        isDayOfWeek ? dayOfWeekString : openTime))
                )
                .setActions(new OneButtonAction(R.string.ok, null))
                .show(getFragmentManager(), null);
    }

    /**
     * @return A transition listener which will impact the action bar overlay's alpha.
     */
    private CategoriesView.TransitionListener createCategoriesTransitionListener() {
        return new CategoriesView.TransitionListener() {
            Drawable wrappedLogo = DrawableCompat.wrap(actionBarLogo.getDrawable());

            int logoFromR;
            int logoFromG;
            int logoFromB;
            int logoFromA;
            int logoToR;
            int logoToG;
            int logoToB;
            int logoToA;
            Interpolator logoTintInterpolator;

            {
                Resources res = getResources();
                Resources.Theme theme = getTheme();

                int logoFrom = ResourcesCompat.getColor(res, R.color.action_bar_logo_light, theme);
                logoFromR = Color.red(logoFrom);
                logoFromG = Color.green(logoFrom);
                logoFromB = Color.blue(logoFrom);
                logoFromA = Color.alpha(logoFrom);

                int logoTo = ResourcesCompat.getColor(res, R.color.action_bar_logo_dark, theme);

                logoToR = Color.red(logoTo);
                logoToG = Color.green(logoTo);
                logoToB = Color.blue(logoTo);
                logoToA = Color.alpha(logoTo);

                logoTintInterpolator = new Interpolator() {
                    @Override
                    public float getInterpolation(float v) {
                        if (v < 0.5f) {
                            v *= 2;
                            return (v * v * v) * 0.5f;
                        }

                        v *= 2;
                        v -= 2;
                        return (v * v * v + 2) * 0.5f;
                    }
                };
            }

            @Override
            public void onValueUpdated(float newVal) {
                actionBarOverlay.setAlpha(1f - newVal);

                newVal = logoTintInterpolator.getInterpolation(newVal);

                int modulatedLogoR = (int) (logoFromR + (logoToR - logoFromR) * newVal);
                int modulatedLogoG = (int) (logoFromG + (logoToG - logoFromG) * newVal);
                int modulatedLogoB = (int) (logoFromB + (logoToB - logoFromB) * newVal);
                int modulatedLogoA = (int) (logoFromA + (logoToA - logoFromA) * newVal);

                DrawableCompat.setTint(wrappedLogo, Color.argb(modulatedLogoA, modulatedLogoR, modulatedLogoG, modulatedLogoB));
            }
        };
    }

    private void setupLayoutBackground() {
        backgroundItems = new View[]{
                findViewById(R.id.background_item_1),
                findViewById(R.id.background_item_2),
                findViewById(R.id.background_item_3),
                findViewById(R.id.background_item_4)
        };

        backgroundItemParallax = new float[]{
                1,
                0.95f,
                0.9f,
                0.975f,
                0.925f
        };
    }

    public void scrollBackgroundBy(float yDelta) {
        float newTranslationY;
        int parentHeight = -1;
        View v;
        for (int i = 0, n = backgroundItems.length; i < n; i++) {
            v = backgroundItems[i];

            newTranslationY = v.getTranslationY() + yDelta * backgroundItemParallax[i];
            if (parentHeight == -1) {
                parentHeight = ((ViewGroup) v.getParent()).getMeasuredHeight();
            }

            if (yDelta < 0 ? newTranslationY < -v.getMeasuredHeight() : newTranslationY > parentHeight) {
                newTranslationY = yDelta < 0 ? newTranslationY + parentHeight + v.getMeasuredHeight() :
                        newTranslationY - parentHeight - v.getMeasuredHeight();
            }

            v.setTranslationY(newTranslationY);
        }
    }

    private void maybeShowTutorial() {
        boolean start = preferences.getBoolean(PreferenceKeys.SHOW_TUTORIAL, true);

        if (start) {
            startTutorial();
        }
    }

    private void startTutorial() {
        preferences.edit().putBoolean(PreferenceKeys.SHOW_TUTORIAL, false).apply();

        new CategoriesTutorial(this);
    }

    private void updateCartCost() {
        double cartCost = ShoppingCartActivity.getCartCost();

        TextView badge = (TextView) findViewById(R.id.cart_cost);

        if (cartCost == 0) {
            //  This won't occur while in MainActivity, no animation needed
            badge.setVisibility(View.GONE);
            cartItemCount = 0;
            return;
        }

        int itemCount = ShoppingCartActivity.getItemCount();

        String costStr = getString(R.string.cart_cost, Formatter.formatCurrency(cartCost));

        badge.setText(costStr);

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

            JSONObject obj = new JSONObject(categoriesJson);
            JSONArray categoriesArray = obj.getJSONArray("items");
            JSONObject category;
            int color;

            String baseImageUrl = obj.getString("base_image_url");

            Resources res = getResources();
            Resources.Theme theme = getTheme();

            int textColorLight = ResourcesCompat.getColor(res, R.color.category_text_light, theme);
            int textColorDark = ResourcesCompat.getColor(res, R.color.category_text_dark, theme);

            boolean isBackgroundDark;

            categories = new CategoriesAdapter.CategoryData[categoriesArray.length()];
            for (int i = 0, n = categoriesArray.length(); i < n; i++) {
                category = categoriesArray.getJSONObject(i);
                color = Color.parseColor("#" + category.getString("color"));

                isBackgroundDark = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255 < 0.75;

                //  Trailing space in name for design
                categories[i] = new CategoriesAdapter.CategoryData(
                        category.getInt("id"),
                        category.getString("name") + " ",
                        color,
                        isBackgroundDark ? textColorLight : textColorDark,
                        category.has("background") ?
                                Caches.getBitmapFromCache(
                                        baseImageUrl + category.getString("background")
                                ) : null
                );
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

        currentArticlePage = 0;
        fetchArticles();

        //changeBackgroundColor(categoryData.backgroundColor);

        boolean isFrontpage = selectedCategories.isEmpty();

        TextView categoriesHeader = (TextView) findViewById(R.id.category_header);

        if (isFrontpage) {
            categoriesHeader.setText(R.string.popular_this_week);
        } else {
            ArrayList<String> categoryNames = new ArrayList<>();
            for (int i = 0, n = categories.length; i < n; i++) {
                if (categories[i].enabled) {
                    categoryNames.add(categories[i].name.trim());
                }
            }
            categoriesHeader.setText(StringUtils.glue(this, categoryNames.toArray(new String[0])));
        }

        hideDragNoteViews();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    private boolean canFetchArticles() {
        return !fetchingArticles && !fetchedLastArticlePage;
    }

    public void fetchArticles() {
        if (!canFetchArticles()) {
            return;
        }

        fetchingArticles = true;

        setNoItemsVisibility(false);

        StringBuilder queryString = new StringBuilder("out=articles");
        queryString.append("&count=").append(articlesPerPage);
        queryString.append("&page=").append(currentArticlePage);

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

        setArticleFetchIndicatorVisibility(true);

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

    private void setNoItemsVisibility(boolean visible) {
        findViewById(R.id.no_items).setVisibility(visible ? View.VISIBLE : View.GONE);
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

            ArticlesView articlesView = (ArticlesView) findViewById(R.id.articles);
            ArticlesAdapter adapter;
            ArticlesAdapter.ArticleData[] itemsArray = items.toArray(new ArticlesAdapter.ArticleData[0]);
            if (articlesView.getAdapter() == null) {
                adapter = new ArticlesAdapter(itemsArray);
                articlesView.setAdapter(adapter);
                adapter.setDarkMode(true);

                articlesView.setAdapter(adapter);
            } else {
                adapter = (ArticlesAdapter) articlesView.getAdapter();
                ArrayList<ArticlesAdapter.ArticleData> adapterItems = adapter.getItems();

                int oldLen = adapterItems.size();

                if (currentArticlePage == 0) {
                    //  Clear all
                    adapterItems.clear();
                    adapterItems.addAll(items);

                    adapter.notifyDataSetChanged();
                } else {
                    if (itemsArray.length > 0) {
                        //  Append
                        adapterItems.addAll(items);
                        adapter.notifyItemRangeInserted(oldLen, items.size());
                    } else {
                        fetchedLastArticlePage = true;
                    }
                }
            }

        } catch (JSONException ex) {
            //  TODO: Fix when touching notification
            //  TODO: Handle JSON error
            setNoItemsVisibility(true);
        }

        if (fetchedLastArticlePage) {
            //  Remove padding used for loading indication purposes
            ArticlesView articles = (ArticlesView) findViewById(R.id.articles);
            articles.setPadding(0, articles.getPaddingTop(), 0, 0);
        }

        fetchingArticles = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setArticleFetchIndicatorVisibility(false);
            }
        });
    }

    private void setArticleFetchIndicatorVisibility(boolean visible) {
        ViewPropertyAnimator anim = articlesFetchIndicatorView.animate();
        anim.cancel();

        articlesFetchIndicatorView.setVisibility(View.VISIBLE);
        articlesFetchIndicatorView.setAlpha(visible ? 0 : 1);

        anim.alpha(visible ? 1 : 0).setDuration(150);
        anim.setListener(visible ? null : new AnimatorAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                articlesFetchIndicatorView.setVisibility(View.GONE);
            }
        });
        anim.start();
    }

    private void keepArticlesScroll(ArticlesView view) {
        final LinearLayoutManager layoutManager = (LinearLayoutManager) view.getLayoutManager();
        final int lastItem = layoutManager.findLastCompletelyVisibleItemPosition();
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                view.removeOnLayoutChangeListener(this);

                layoutManager.scrollToPosition(lastItem);
            }
        });
    }

    @Override
    public void showArticleInfo(ArticlesAdapter.ArticleData article) {
        ArticleInfoFragment fragment = new ArticleInfoFragment();
        fragment.fetchArticleInfo(article);
        fragment.show(getFragmentManager(), null);
    }

    public void showShoppingCart(View v) {
        if (ShoppingCartActivity.getItemCount() == 0) {
            //  No items
            new AlertDialog()
                    .setHeader(getString(R.string.shopping_cart_empty_header))
                    .setText(getString(R.string.shopping_cart_empty))
                    .setActions(new OneButtonAction(R.string.ok, null))
                    .show(getFragmentManager(), null);
            return;
        }

        Intent i = new Intent(this, ShoppingCartActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.shopping_cart_reveal, android.R.anim.fade_out);
    }

    /**
     * Shows the user where to drag an article.
     */
    public void showDraggingHints(@Nullable View v) {
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

        dragNotesShowCount++;

        delayDragNoteViewsHide();

        showingNoteViews = true;
    }

    private void delayDragNoteViewsHide() {
        final int GENERATION = dragNotesShowCount;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dragNotesShowCount == GENERATION) {
                    //   Not shown since
                    hideDragNoteViews();
                }
            }
        }, 3500);
    }

    private void hideDragNoteViews() {
        if (!showingNoteViews) {
            return;
        }

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
            showDraggingHints(null);
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

    @Override
    public void onArticleRemoved(ArticlesAdapter.ArticleData data) {

    }

    @Override
    public void onArticleClicked(ArticleView view) {
        showDraggingHints(null);
    }
}
