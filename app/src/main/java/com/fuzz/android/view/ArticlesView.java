package com.fuzz.android.view;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.activity.ShoppingCartActivity;
import com.fuzz.android.adapter.ArticlesAdapter;
import com.fuzz.android.animator.AnimatorAdapter;

/**
 * View for displaying articles.
 */
public class ArticlesView extends RecyclerView {
    private static final boolean DEBUG = true;
    public static Rect shoppingCartBtnBounds;
    public static Rect articleInfoBtnBounds;
    public static Rect draggableArticleBounds;
    private CategoriesView categoriesView;
    private ViewPropertyAnimator draggableAnimator;
    private ArticleView draggableArticleView;
    private ArticleView selectedArticleView;
    private Interpolator[] pickUpInterpolators;
    private Interpolator draggableReturnInterpolator;
    /**
     * Interpolator when draggable disappears.
     */
    private Interpolator draggableDisappearInterpolator;
    private float oldTouchX = -1;
    private float oldTouchY = -1;
    /**
     * Listener for when requesting additional article info.
     */
    private ArticleInfoListener articleInfoListener;
    private boolean itemsMovable;
    private boolean scrollable = true;
    private boolean draggableOverCart;
    private boolean draggableOverInfo;
    private View cartBtnBackground;
    private View infoBtnBackground;
    private ArticleDragListener articleDragListener;
    private ArticlesContainerView container;
    private android.os.Handler handler;
    private int articleHoldDownDelay = 150;
    private int numOfTouchUps;

    public ArticlesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        GridLayoutManager layout = new GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false) {
            @Override
            public boolean canScrollVertically() {
                return scrollable && !isDraggingArticle() && super.canScrollVertically();
            }
        };
        setLayoutManager(layout);

        handler = new Handler();
    }

    public ArticlesContainerView getContainer() {
        return container;
    }

    public void setContainer(ArticlesContainerView container) {
        this.container = container;
    }

    public boolean isDraggingArticle() {
        return selectedArticleView != null;
    }

    public ArticleDragListener getArticleDragListener() {
        return articleDragListener;
    }

    public void setArticleDragListener(ArticleDragListener articleDragListener) {
        this.articleDragListener = articleDragListener;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    public boolean isItemsMovable() {
        return itemsMovable;
    }

    public void setItemsMovable(boolean itemsMovable) {
        this.itemsMovable = itemsMovable;
    }

    public ArticleInfoListener getArticleInfoListener() {
        return articleInfoListener;
    }

    public void setArticleInfoListener(ArticleInfoListener articleInfoListener) {
        this.articleInfoListener = articleInfoListener;
    }

    public CategoriesView getCategoriesView() {
        return categoriesView;
    }

    public void setCategoriesView(CategoriesView categoriesView) {
        this.categoriesView = categoriesView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (itemsMovable) {
            int action = e.getAction();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    View under = findChildViewUnder(e.getX(), e.getY());
                    if (under != null) {
                        attemptPickUpArticle((ArticleView) under);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (selectedArticleView != null) {
                        moveDraggableArticleView(e);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (selectedArticleView != null) {
                        restoreDraggableArticleView();
                    }

                    oldTouchX = -1;
                    oldTouchY = -1;

                    numOfTouchUps++;
                    break;
            }
        } else {
            return super.onInterceptTouchEvent(e);
        }

        return false;
    }

    private void restoreDraggableArticleView() {
        final ArticleView selectedArticle = selectedArticleView;

        if (draggableOverCart) {
            onAddedToShoppingCart(selectedArticleView);

            animateButtonBackground(cartBtnBackground, false);

            //  Animate added to cart
            if (draggableDisappearInterpolator == null) {
                draggableDisappearInterpolator = new AnticipateInterpolator();
            }

            View draggable = draggableArticleView;
            draggableAnimator.cancel();
            draggableAnimator = draggable.animate()
                    .scaleX(0)
                    .scaleY(0)
                    .setInterpolator(draggableDisappearInterpolator);
            draggableAnimator.start();

            View selected = selectedArticleView;
            selected.setScaleX(0);
            selected.setScaleY(0);
            selected.setAlpha(1);
            selected.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .start();

        } else {
            if (draggableReturnInterpolator == null) {
                //  One-time setup
                draggableReturnInterpolator = new AccelerateDecelerateInterpolator();
            }
            if (draggableOverInfo) {
                //  Did drag to article info
                articleInfoListener.showArticleInfo((ArticlesAdapter.ArticleData) selectedArticleView.getTag());

                animateButtonBackground(infoBtnBackground, false);
            } else {
                //  Dragged to nowhere
                articleDragListener.onMissedDrag(selectedArticle);
            }

            int[] selectedViewLocation = new int[2];
            selectedArticleView.getLocationOnScreen(selectedViewLocation);

            draggableAnimator.cancel();
            draggableAnimator = draggableArticleView.animate().
                    translationX(selectedViewLocation[0])
                    .translationY(selectedViewLocation[1])
                    .setInterpolator(draggableReturnInterpolator)
                    .setListener(new AnimatorAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            ViewParent draggableViewParent = draggableArticleView.getParent();
                            if (draggableViewParent != null) {
                                ((ViewGroup) draggableArticleView.getParent()).removeView(draggableArticleView);
                            }
                            selectedArticle.setAlpha(1f);

                            //  No longer dragging
                            selectedArticleView = null;
                        }
                    });
            draggableAnimator.start();
        }
    }

    private void onAddedToShoppingCart(ArticleView selected) {
        if (DEBUG) {
            Log.i(getClass().getSimpleName(), "Added an item to the shopping cart");
        }

        ShoppingCartActivity.addToCart((ArticlesAdapter.ArticleData) selected.getTag());
    }

    private void moveDraggableArticleView(MotionEvent ev) {
        float newX = ev.getRawX();
        float newY = ev.getRawY();

        if (oldTouchX < 0) {
            //  Prevent "jumping"
            oldTouchX = newX;
            oldTouchY = newY;
        }

        float deltaX = newX - oldTouchX;
        float deltaY = newY - oldTouchY;

        draggableArticleView.setTranslationX(draggableArticleView.getTranslationX() + deltaX);
        draggableArticleView.setTranslationY(draggableArticleView.getTranslationY() + deltaY);

        draggableArticleBounds.offset((int) deltaX, (int) deltaY);

        oldTouchX = newX;
        oldTouchY = newY;

        Rect cartBnds = shoppingCartBtnBounds;
        Rect infoBnds = articleInfoBtnBounds;
        boolean draggableOverCart = draggableArticleBounds.intersects(cartBnds.left, cartBnds.top, cartBnds.right, cartBnds.bottom);
        boolean draggableOverInfo = draggableArticleBounds.intersects(infoBnds.left, infoBnds.top, infoBnds.right, infoBnds.bottom);

        if (draggableOverCart && !this.draggableOverCart) {
            //  Just entered
            animateButtonBackground(cartBtnBackground, true);
        } else if (!draggableOverCart && this.draggableOverCart) {
            //  Just left
            animateButtonBackground(cartBtnBackground, false);
        }
        this.draggableOverCart = draggableOverCart;

        if (draggableOverInfo && !this.draggableOverInfo) {
            //  Just entered
            animateButtonBackground(infoBtnBackground, true);
        } else if (!draggableOverInfo && this.draggableOverInfo) {
            //  Just left
            animateButtonBackground(infoBtnBackground, false);
        }
        this.draggableOverInfo = draggableOverInfo;
/*
        Log.i("articles", "cart X: " + shoppingCartBtnBounds.left + ", Y: " + shoppingCartBtnBounds.top + ", w: " + shoppingCartBtnBounds.width() + ", h: " + shoppingCartBtnBounds.height());
        Log.i("articles", "info X: " + articleInfoBtnBounds.left + ", Y: " + articleInfoBtnBounds.top + ", w: " + articleInfoBtnBounds.width() + ", h: " + articleInfoBtnBounds.height());
        Log.i("articles", "draggable X: " + draggableArticleBounds.left + ", Y: " + draggableArticleBounds.top + ", w: " + draggableArticleBounds.width() + ", h: " + draggableArticleBounds.height());
        */
    }

    private void animateButtonBackground(final View background, boolean reveal) {
        float scaleTo = reveal ? 1 : 0.25f;

        if (reveal) {
            background.setVisibility(View.VISIBLE);
        }

        Animator.AnimatorListener listener = reveal ? null : new AnimatorAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                background.setVisibility(View.VISIBLE);
            }
        };

        background.animate()
                .alpha(reveal ? 1 : 0)
                .scaleX(scaleTo)
                .scaleY(scaleTo)
                .setListener(listener)
                .start();
    }

    /**
     * Triggers a timer to ensure that touch is held down on an article to pick it up.
     *
     * @param selectedView
     */
    private void attemptPickUpArticle(final ArticleView selectedView) {
        //  Must match on delayed run() = touch not interrupted
        final int NUM_TOUCH_UPS = numOfTouchUps;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (container.getScrollingDirection() == ArticlesContainerView.NONE) {
                    if (NUM_TOUCH_UPS == numOfTouchUps) {
                        onPickedUpArticle(selectedView);
                    }
                }
            }
        }, articleHoldDownDelay);
    }

    private void onPickedUpArticle(ArticleView selectedView) {
        selectedArticleView = selectedView;
        selectedView.setPickedUp(true);

        if (articleDragListener != null) {
            articleDragListener.onStartedDrag(selectedView);
        }

        ViewGroup.LayoutParams layoutParams;

        if (draggableArticleView == null) {
            //  Do initial setup
            draggableArticleView = (ArticleView) LayoutInflater.from(getContext()).inflate(R.layout.article_item, null, false);
            layoutParams = new FrameLayout.LayoutParams(selectedView.getMeasuredWidth(), selectedView.getMeasuredHeight());
            draggableArticleBounds = new Rect();

            int[] viewLocation = new int[2];

            //  Get bounds for shopping cart button
            shoppingCartBtnBounds = new Rect();
            View shoppingCartBtn = getRootView().findViewById(R.id.shopping_cart_button);
            shoppingCartBtn.getLocationInWindow(viewLocation);

            shoppingCartBtnBounds.set(viewLocation[0], viewLocation[1],
                    viewLocation[0] + shoppingCartBtn.getMeasuredWidth(), viewLocation[1] + shoppingCartBtn.getMeasuredHeight());

            //  Get bounds for article info button
            articleInfoBtnBounds = new Rect();
            View articleInfoBtn = getRootView().findViewById(R.id.article_info_button);
            articleInfoBtn.getLocationInWindow(viewLocation);

            articleInfoBtnBounds.set(viewLocation[0], viewLocation[1],
                    viewLocation[0] + articleInfoBtn.getMeasuredWidth(), viewLocation[1] + articleInfoBtn.getMeasuredHeight());

            pickUpInterpolators = new Interpolator[]{
                    new DecelerateInterpolator(),
                    new BounceInterpolator()
            };

            View root = getRootView();
            cartBtnBackground = root.findViewById(R.id.shopping_cart_btn_bg);
            infoBtnBackground = root.findViewById(R.id.article_info_btn_bg);

        } else {
            ViewParent draggableParent = draggableArticleView.getParent();
            if (draggableParent != null) {
                ((ViewGroup) draggableParent).removeView(draggableArticleView);
            }
            layoutParams = draggableArticleView.getLayoutParams();
            layoutParams.width = selectedView.getMeasuredWidth();
            layoutParams.height = selectedView.getMeasuredHeight();
        }

        ViewGroup root = (ViewGroup) getRootView();
        root.addView(draggableArticleView);
        draggableArticleView.setLayoutParams(layoutParams);

        int[] selectedLocation = new int[2];
        selectedView.getLocationOnScreen(selectedLocation);

        draggableArticleView.setScaleX(1);
        draggableArticleView.setScaleY(1);
        draggableArticleView.setTranslationX(selectedLocation[0]);
        draggableArticleView.setTranslationY(selectedLocation[1]);

        draggableArticleBounds.set(selectedLocation[0], selectedLocation[1], selectedLocation[0] + layoutParams.width, selectedLocation[1] + layoutParams.height);

        //  center-ish
        draggableArticleBounds.offset(0, (int) (-draggableArticleBounds.height() * 0.25f));
        copyArticleViews(selectedView, draggableArticleView);

        //  Hide, but don't obstruct touch event
        selectedView.setAlpha(0f);

        //  Apply pickup animation to draggable
        if (draggableAnimator != null) {
            draggableAnimator.cancel();
        }
        draggableAnimator = draggableArticleView.animate()
                .scaleY(1.1f)
                .scaleX(1.1f)
                .setInterpolator(pickUpInterpolators[0])
                .setDuration(200)
                .setListener(new AnimatorAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        draggableArticleView.animate()
                                .scaleX(1)
                                .scaleY(1)
                                .setListener(null)
                                .setDuration(1000)
                                .setInterpolator(pickUpInterpolators[1])
                                .start();
                    }
                });
    }

    private void copyArticleViews(ArticleView from, ArticleView to) {
        TextView fromQuantity = (TextView) from.findViewById(R.id.quantity);
        TextView toQuantity = (TextView) to.findViewById(R.id.quantity);

        toQuantity.setText(fromQuantity.getText());

        TextView fromCost = (TextView) from.findViewById(R.id.cost);
        TextView toCost = (TextView) to.findViewById(R.id.cost);

        fromCost.setText(toCost.getText());

        TextView fromName = (TextView) from.findViewById(R.id.article_name);
        TextView toName = (TextView) to.findViewById(R.id.article_name);

        toName.setText(fromName.getText());

        ImageView fromImage = (ImageView) from.findViewById(R.id.image);
        ImageView toImage = (ImageView) to.findViewById(R.id.image);

        toImage.setImageDrawable(fromImage.getDrawable());
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        ((ArticlesAdapter) adapter).setViewLayoutManager((GridLayoutManager) getLayoutManager());
    }

    public interface ArticleInfoListener {
        public void showArticleInfo(ArticlesAdapter.ArticleData article);
    }

    /**
     * Used to listen when an article is dragged to nowhere.
     */
    public interface ArticleDragListener {
        public void onStartedDrag(ArticleView view);

        /**
         * Called when an article view has been dragged to an unknown location.
         *
         * @param view
         */
        public void onMissedDrag(ArticleView view);
    }
}
