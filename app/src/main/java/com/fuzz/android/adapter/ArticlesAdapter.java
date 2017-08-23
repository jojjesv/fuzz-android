package com.fuzz.android.adapter;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.format.Formatter;
import com.fuzz.android.net.Caches;
import com.fuzz.android.view.DefaultTypefaces;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Johan on 2017-07-26.
 */

public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticlesViewHolder> {

    private RecyclerView.LayoutManager viewLayoutManager;
    private ArrayList<ArticleData> articles;
    private View.OnClickListener itemsOnClickListener;
    private boolean darkMode;

    public ArticlesAdapter(ArticleData[] data) {
        articles = new ArrayList<>(Arrays.asList(data));
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public View.OnClickListener getItemsOnClickListener() {
        return itemsOnClickListener;
    }

    public void setItemsOnClickListener(View.OnClickListener itemsOnClickListener) {
        this.itemsOnClickListener = itemsOnClickListener;
    }

    public void setViewLayoutManager(RecyclerView.LayoutManager viewLayoutManager) {
        this.viewLayoutManager = viewLayoutManager;
    }

    @Override
    public ArticlesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.article_item, parent, false);

        DefaultTypefaces.applyDefaultsToChildren((ViewGroup) v);
        ArticlesViewHolder holder = new ArticlesViewHolder(v);

        if (darkMode) {
            int lightColor = parent.getResources().getColor(R.color.white);
            holder.costView.setTextColor(lightColor);
            holder.nameView.setTextColor(lightColor);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(ArticlesViewHolder holder, int position) {
        final ArticleData data = articles.get(position);

        holder.itemView.setOnClickListener(itemsOnClickListener);
        holder.itemView.setTag(data);

        if (data.image == null && !data.fetchingImage) {
            data.fetchingImage = true;

            final int POSITION = position;
            final ArticlesViewHolder HOLDER = holder;

            Caches.getBitmapFromUrl(data.imageUrl, new Caches.CacheCallback<Bitmap>() {
                @Override
                public void onGotItem(Bitmap item, boolean wasCached) {
                    int firstVisibleItemPosition;
                    int lastVisibleItemPosition;

                    if (viewLayoutManager instanceof GridLayoutManager) {
                        GridLayoutManager cast = (GridLayoutManager) viewLayoutManager;
                        firstVisibleItemPosition = cast.findFirstVisibleItemPosition();
                        lastVisibleItemPosition = cast.findLastVisibleItemPosition();
                    } else {
                        LinearLayoutManager cast = (LinearLayoutManager) viewLayoutManager;
                        firstVisibleItemPosition = cast.findFirstVisibleItemPosition();
                        lastVisibleItemPosition = cast.findLastVisibleItemPosition();
                    }

                    if (firstVisibleItemPosition <= POSITION && lastVisibleItemPosition >= POSITION) {
                        //  Item visible
                        if (data.imageDrawable == null) {
                            View refView = HOLDER.imageView;
                            Bitmap scaled = Bitmap.createScaledBitmap(item,
                                    refView.getMeasuredWidth(),
                                    refView.getMeasuredHeight(), true);
                            data.createImageDrawable(HOLDER.imageView.getResources(), scaled);
                        }
                        HOLDER.imageView.setImageDrawable(data.imageDrawable);
                    }

                    data.image = item;
                }
            });
        } else if (data.image != null) {
            if (data.imageDrawable == null) {
                data.createImageDrawable(holder.imageView.getResources(), data.image);
            }
            holder.imageView.setImageDrawable(data.imageDrawable);
        }

        if (data.costString == null) {
            data.costString = holder.itemView.getResources().getString(R.string.cost, Formatter.formatCurrency(data.cost));
        }

        holder.costView.setText(data.costString);
        holder.nameView.setText(data.name);

        boolean showQuantity = data.quantity > 1;
        holder.quantityView.setVisibility(showQuantity ? View.VISIBLE : View.GONE);
        if (showQuantity) {
            if (data.quantityString == null){
                data.quantityString = holder.itemView.getResources().getString(R.string.quantity, data.quantity);
            }

            holder.quantityView.setText(data.quantityString);
        }

        holder.newBadge.setVisibility(data.isNew ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public ArrayList<ArticleData> getItems() {
        return articles;
    }

    public static class ArticleData {
        public int id;
        public int quantity;
        public double cost;
        public String imageUrl;
        public String name;
        private Bitmap image;
        private boolean fetchingImage;
        public boolean isNew;
        private String costString;
        private RoundedBitmapDrawable imageDrawable;
        private String quantityString;

        public ArticleData(int id, int quantity, double cost, String imageUrl, String name) {
            this.id = id;
            this.quantity = quantity;
            this.cost = cost;
            this.imageUrl = imageUrl;
            this.name = name;
        }

        private void createImageDrawable(Resources res, Bitmap bitmap) {
            imageDrawable = RoundedBitmapDrawableFactory.create(res, bitmap);
            imageDrawable.setCornerRadius(24f);
        }
    }

    public static class ArticlesViewHolder extends RecyclerView.ViewHolder {
        public TextView nameView;
        public ImageView imageView;
        public TextView quantityView;
        public TextView costView;
        public View newBadge;

        public ArticlesViewHolder(View itemView) {
            super(itemView);

            nameView = (TextView) itemView.findViewById(R.id.article_name);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            quantityView = (TextView) itemView.findViewById(R.id.quantity);
            costView = (TextView) itemView.findViewById(R.id.cost);
            newBadge = (TextView) itemView.findViewById(R.id.new_badge);
        }
    }
}
