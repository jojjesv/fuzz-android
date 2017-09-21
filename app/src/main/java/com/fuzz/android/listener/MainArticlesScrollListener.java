package com.fuzz.android.listener;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.fuzz.android.adapter.ArticlesAdapter;

/**
 * Created by Johan on 2017-08-31.
 */
public class MainArticlesScrollListener implements ScrollUpdateListener {
    private FetchNextPageListener listener;

    public MainArticlesScrollListener(FetchNextPageListener listener) {
        this.listener = listener;
    }

    @Override
    public void onScrollChanged(RecyclerView recyclerView) {
        if (recyclerView.getChildCount() == 0) {
            return;
        }

        int lastChildBottom = recyclerView.getChildAt(recyclerView.getChildCount() - 1).getBottom();

        if (lastChildBottom == recyclerView.getBottom()) {
            //  Inspect layoutmanager
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            ArticlesAdapter adapter = (ArticlesAdapter) recyclerView.getAdapter();

            if (layoutManager.findLastVisibleItemPosition() == adapter.getItemCount() - 1) {
                //  At bottom
                listener.fetchNextPage();
            }
        }
    }

    public interface FetchNextPageListener {
        public void fetchNextPage();
    }
}
