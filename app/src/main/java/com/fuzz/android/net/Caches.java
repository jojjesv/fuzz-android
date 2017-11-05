package com.fuzz.android.net;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;


import com.fuzz.android.R;

import java.util.HashMap;

/**
 * Provides methods for caching some objects.
 */
public class Caches {
    private static HashMap<String, Bitmap> bmpCache = new HashMap<>();
    private static Context context;

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        Caches.context = context;
    }

    public static void getBitmapFromUrl(final String url, final CacheCallback<Bitmap> callback) {
        Bitmap cached = bmpCache.get(url);
        if (cached != null) {
            callback.onGotItem(cached, true);
        }

        //  Fetch bitmap
        BitmapDownloadTask dl = new BitmapDownloadTask() {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);

                bmpCache.put(url, bitmap);
                callback.onGotItem(bitmap, false);
            }
        };
        dl.execute(url);
    }

    @Nullable
    public static Bitmap getBitmapFromCache(final String url) {
        Bitmap cached = bmpCache.get(url);
        return cached;
    }

    public static void replaceInBitmapCache(String url, Bitmap newBitmap) {
        bmpCache.put(url, newBitmap);
    }

    public interface CacheCallback<T> {
        void onGotItem(T item, boolean wasCached);
    }
}
