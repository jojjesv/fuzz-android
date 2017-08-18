package com.fuzz.android.fragment;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.adapter.ArticlesAdapter;
import com.fuzz.android.backend.BackendCom;
import com.fuzz.android.backend.ResponseCodes;
import com.fuzz.android.net.Caches;
import com.fuzz.android.view.DefaultTypefaces;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Shows info about a specific article.
 */
public class ArticleInfoFragment extends DialogFragment {
    private View view;
    private ArticlesAdapter.ArticleData article;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.article_info, container, false);
        view = v;

        TextView nameView = (TextView)v.findViewById(R.id.article_name);
        nameView.setText(article.name);

        DefaultTypefaces.applyDefaultsToChildren((ViewGroup)v);

        Caches.getBitmapFromUrl(article.imageUrl, new Caches.CacheCallback<Bitmap>() {
            @Override
            public void onGotItem(Bitmap item, boolean wasCached) {
                ImageView image = (ImageView)view.findViewById(R.id.image);

                if (wasCached){
                    image.setAlpha(0f);
                    image.animate()
                            .alpha(1)
                            .start();
                }

                image.setImageBitmap(item);
            }
        });

        return v;
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
        try {

            JSONObject infoObj = new JSONObject(response);

            boolean hasDescription = infoObj.has("description");
            boolean hasContents = infoObj.has("contents");

            TextView descriptionView = (TextView) view.findViewById(R.id.description);
            TextView contentsView = (TextView) view.findViewById(R.id.contents);

            if (!hasDescription && hasContents) {
                //  No description
                descriptionView.setVisibility(View.GONE);
            } else if (hasDescription && !hasContents) {
                //  No contents info
                contentsView.setVisibility(View.GONE);
            }

            if (hasDescription) {
                descriptionView.setText(infoObj.getString("description"));
            } else if (!hasContents) {
                //  has neither
                descriptionView.setText(getString(R.string.no_description_or_contents));
                contentsView.setVisibility(View.GONE);
            } else {
                contentsView.setText(infoObj.getString("contents"));
            }

        } catch (JSONException ex) {
            //  TODO: Handle JSON error
        }
    }
}
