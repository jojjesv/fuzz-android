package com.fuzz.android.adapter;

import android.graphics.Bitmap;
import android.support.v7.widget.GridLayoutManager;
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

    private GridLayoutManager viewLayoutManager;
    private ArrayList<ArticleData> articles;
    private View.OnClickListener itemsOnClickListener;

    public ArticlesAdapter(ArticleData[] data) {
        articles = new ArrayList<>(Arrays.asList(data));
    }

    public View.OnClickListener getItemsOnClickListener() {
        return itemsOnClickListener;
    }

    public void setItemsOnClickListener(View.OnClickListener itemsOnClickListener) {
        this.itemsOnClickListener = itemsOnClickListener;
    }

    public GridLayoutManager getViewLayoutManager() {
        return viewLayoutManager;
    }

    public void setViewLayoutManager(GridLayoutManager viewLayoutManager) {
        this.viewLayoutManager = viewLayoutManager;
    }

    @Override
    public ArticlesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.article_item, parent, false);
        DefaultTypefaces.applyDefaultsToChildren((ViewGroup) v);
        return new ArticlesViewHolder(v);
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
                    if (viewLayoutManager.findFirstVisibleItemPosition() <= POSITION && viewLayoutManager.findLastVisibleItemPosition() >= POSITION) {
                        //  Item visible
                        HOLDER.imageView.setImageBitmap(item);
                    }

                    data.image = item;
                }
            });
        } else if (data.image != null) {
            holder.imageView.setImageBitmap(data.image);
        }

        if (data.costString == null){
            data.costString = holder.itemView.getResources().getString(R.string.cost, Formatter.formatCurrency(data.cost));
        }

        holder.costView.setText(data.costString);
        holder.nameView.setText(data.name);

        boolean showQuantity = data.quantity > 1;
        holder.quantityView.setVisibility(showQuantity ? View.VISIBLE : View.GONE);
        if (showQuantity) {
            holder.quantityView.setText(String.valueOf(data.quantity));
        }
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
        private String costString;

        public ArticleData(int id, int quantity, double cost, String imageUrl, String name) {
            this.id = id;
            this.quantity = quantity;
            this.cost = cost;
            this.imageUrl = imageUrl;
            this.name = name;
        }
    }

    public static class ArticlesViewHolder extends RecyclerView.ViewHolder {
        public TextView nameView;
        public ImageView imageView;
        public TextView quantityView;
        public TextView costView;

        public ArticlesViewHolder(View itemView) {
            super(itemView);

            nameView = (TextView) itemView.findViewById(R.id.article_name);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            quantityView = (TextView) itemView.findViewById(R.id.quantity);
            costView = (TextView) itemView.findViewById(R.id.cost);
        }
    }
}
