package com.fuzz.android.fragment;

import android.animation.Animator;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.adapter.ArticlesAdapter;
import com.fuzz.android.animator.AnimatorAdapter;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.net.Caches;
import com.fuzz.android.view.DefaultTypefaces;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Shows info about a specific article.
 */
public class ArticleInfoFragment extends BaseDialogFragment {
    private View view;
    private ArticlesAdapter.ArticleData article;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.article_info, container, false);
        view = v;

        TextView nameView = (TextView) v.findViewById(R.id.article_name);
        nameView.setText(getResources().getString(R.string.article_name_quantity, article.name, article.quantity));

        DefaultTypefaces.applyDefaultsToChildren((ViewGroup) v);

        final ImageView image = (ImageView) view.findViewById(R.id.image);
        image.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                setupImage(image);
                image.removeOnLayoutChangeListener(this);
            }
        });

        return v;
    }

    private void setupImage(final ImageView image) {
        Caches.getBitmapFromUrl(article.imageUrl, new Caches.CacheCallback<Bitmap>() {
            @Override
            public void onGotItem(Bitmap item, boolean wasCached) {

                Bitmap scaled = Bitmap.createScaledBitmap(item, image.getMeasuredWidth(), image.getMeasuredHeight(), true);

                if (!wasCached) {
                    image.setAlpha(0f);
                    image.animate()
                            .alpha(1)
                            .start();
                }

                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(image.getResources(), scaled);
                drawable.setCornerRadius(24);
                image.setImageDrawable(drawable);
            }
        });
    }

    public void fetchArticleInfo(ArticlesAdapter.ArticleData article) {
        this.article = article;

        BackendCom.request("out=article_info&id=" + article.id, (byte[]) null, new BackendCom.RequestCallback() {
            @Override
            public void onResponse(String response) {
                parseArticleInfo(response);
            }

            @Override
            public void onFailed() {
                onResponse("" + ResponseCodes.FAILED);
            }
        });
    }

    private void parseArticleInfo(String response) {
        boolean failed = ("" + ResponseCodes.FAILED).contentEquals(response);

        try {

            JSONObject infoObj = null;
            if (!failed) {
                infoObj = new JSONObject(response);
            }
            boolean hasDescription = !failed && infoObj.has("description");
            boolean hasContents = !failed && infoObj.has("contents");

            if (view == null){
                //  If no connection
                dismiss();
                return;
            }

            TextView descriptionView = (TextView) view.findViewById(R.id.description);
            TextView contentsView = (TextView) view.findViewById(R.id.contents);

            if (!hasDescription) {
                //  No description
                descriptionView.setVisibility(View.GONE);
            } else {
                descriptionView.setText(infoObj.getString("description"));
            }

            if (hasContents) {
                contentsView.setText(getString(R.string.contents, infoObj.getString("contents")));
            } else {
                contentsView.setText(R.string.no_contents_info);
            }

            showInfo();

        } catch (JSONException ex) {
            parseArticleInfo("" + ResponseCodes.FAILED);
        }
    }

    /**
     * Shows info, hides loading once info has been parsed.
     */
    private void showInfo() {
        final View root = view.findViewById(R.id.root);

        root.setVisibility(View.VISIBLE);
        root.setAlpha(0);
        root.animate().alpha(1).start();

        final View loading = view.findViewById(R.id.loading);
        loading.animate().alpha(0).setListener(new AnimatorAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                ((ViewGroup) loading.getParent()).removeView(loading);
            }
        }).start();
    }
}
