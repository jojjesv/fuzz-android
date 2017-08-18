package com.fuzz.android.net;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Task for downloading a bitmap. For params, first must be a URL string, and second may be an {@link android.graphics.BitmapFactory.Options} (optional).
 */
public class BitmapDownloadTask extends AsyncTask<Object, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(Object... params) {
        URL url;

        try {
            url = new URL(params[0].toString());
        } catch (MalformedURLException ex) {
            return null;
        }

        URLConnection connection;
        try {
            connection = url.openConnection();

            BitmapFactory.Options bmpOpts;
            if (params.length > 1) {
                bmpOpts = (BitmapFactory.Options) params[1];
            } else {
                bmpOpts = new BitmapFactory.Options();
            }

            Bitmap out = BitmapFactory.decodeStream(connection.getInputStream(), null, bmpOpts);

            return out;
        } catch (IOException ex) {
            return null;
        }
    }

    protected void onError() {

    }
}
