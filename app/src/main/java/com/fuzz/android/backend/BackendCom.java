package com.fuzz.android.backend;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Communication with backend.
 */
public class BackendCom {
    public static final boolean DEBUG = true;

    private static StringBuilder tmpBuilder = new StringBuilder();

    /**
     * Performs a user sensitive request to the backend.
     *
     * @param queryString      Full query string (GET), excluding leading question mark.
     * @param postData
     * @param responseCallback
     * @return Whether the request was successful.
     */
    public static void request(final String queryString, final @Nullable String postData, final RequestCallback responseCallback) {
        request(queryString, postData == null ? null : postData.getBytes(), responseCallback);
    }

    /**
     * Performs a user sensitive request to the backend.
     *
     * @param queryString      Full query string (GET), excluding leading question mark.
     * @param postData
     * @param responseCallback
     * @param fileExtension    Optional file extension if is file upload.
     * @return Whether the request was successful.
     */
    public static void request(final String queryString, final @Nullable byte[] postData, final @Nullable String fileExtension, @Nullable final RequestCallback responseCallback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {

                tmpBuilder.setLength(0);
                tmpBuilder.append(ApiSettings.BACKEND_URL).append(queryString);
                if (DEBUG) {
                    tmpBuilder.append("&debug");
                }

                try {
                    //  http://androidexample.com/Upload_File_To_Server_-_Android_Example/index.php?view=article_discription&aid=83
                    URL url = new URL(tmpBuilder.toString());
                    tmpBuilder.setLength(0);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setDoOutput(postData != null);
                    connection.setUseCaches(false);

                    if (postData != null && postData.length > 0) {
                        boolean isFileUpload = fileExtension != null;
                        String fileName = isFileUpload ? "0." + fileExtension : null;

                        connection.setRequestMethod("POST");
                        if (isFileUpload) {
                            connection.addRequestProperty("Content-Type", "image/jpg");
                            connection.setRequestProperty("Connection", "Keep-Alive");
                            connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");
                            connection.setRequestProperty("uploaded_file", fileName);
                        }

                        DataOutputStream postDataStream = null;
                        OutputStream outStream = connection.getOutputStream();

                        if (isFileUpload) {
                            postDataStream = new DataOutputStream(outStream);    //  instantiated if needed

                            postDataStream.writeBytes("--*****\r\n");
                            postDataStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"\r\n");
                            postDataStream.writeBytes("\r\n");
                        }
                        outStream.write(postData);
                        if (isFileUpload) {
                            postDataStream.writeBytes("\r\n");
                            postDataStream.writeBytes("--******--\r\n");
                        }
                    }
                    BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String l;
                    while ((l = responseReader.readLine()) != null) {
                        tmpBuilder.append(l).append('\n');
                    }

                    //  Remove trailing newline
                    if (tmpBuilder.length() > 0) {
                        tmpBuilder.setLength(tmpBuilder.length() - 1);
                    }

                } catch (Exception ex) {
                    if (responseCallback != null) {
                        responseCallback.onFailed();
                    }
                }
                return tmpBuilder.toString();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (responseCallback != null) {
                    responseCallback.onResponse(s);
                }
            }
        }.execute();
    }

    /**
     * Performs a user sensitive request to the backend.
     *
     * @param queryString      Full query string (GET), excluding leading question mark.
     * @param postData
     * @param responseCallback
     * @return Whether the request was successful.
     */
    public static void request(final String queryString, final @Nullable byte[] postData, final RequestCallback responseCallback) {
        request(queryString, postData, null, responseCallback);
    }

    /**
     * URL encodes some piece of text.
     *
     * @param text Text to encode.
     * @return
     */
    public static String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (Exception ex) {
            return text;
        }
    }

    /**
     * URL decodes some piece of text.
     *
     * @param text Text to encode.
     * @return
     */
    public static String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (Exception ex) {
            return text;
        }
    }

    public interface RequestCallback {
        void onResponse(String response);

        void onFailed();
    }

    public interface ApiSettings {
        String BACKEND_URL = "http://partlight.tech/scripts/fuzz/backend.php?";
    }
}
