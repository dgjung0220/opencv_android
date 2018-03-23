package com.lge.project1;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.naver.api.security.client.MACManager;
import com.naver.api.util.Type;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;

/**
 * Created by dg.jung on 2018-03-16.
 */

public class NaverLens {

    private final static String TAG = "NaverLens";
    private final static String NAVER_URL = "https://api.scopic.naver.com/lge";
    private String mEncryptedURL;

    private NaverLensCallback mCB;
    private String mPath;
    private NaverLensRequest mTask;
    private HttpURLConnection mConnection;

    public NaverLens(Context context) {
        try {
            String key = null;
            Properties properties = new Properties();
            properties.load(context.getAssets().open("NHNAPIGatewayKey.properties"));

            if (properties.elements().hasMoreElements()) {
                key = (String) properties.elements().nextElement();
            }
            MACManager.initialize(Type.KEY, key);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void requestNaverLense(String path) {
        mPath = path;
        mTask = new NaverLensRequest();
        mTask.execute();
    }

    public void setNaverLensCallback(NaverLensCallback cb) {
        mCB = cb;
    }

    public class NaverLensRequest extends AsyncTask<Void, Void, Void> {

        private String response = "";
        private boolean mCancel;
        private MultipartEntity mReqEntity;

        public void requestCancel() {
            mCancel = true;
            Log.d(TAG, "mCancle = " + mCancel);
        }

        private boolean checkCancel() {
            Log.d(TAG, "sleep start = " + mCancel);
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            return mCancel;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            mCancel = false;

            try {
                mEncryptedURL = MACManager.getEncryptUrl(NAVER_URL);
                Log.d(TAG, "encrypted url : " + mEncryptedURL);
            } catch(Exception e) {
                e.printStackTrace();
            }

            Bitmap bm = BitmapUtil.getNaverBitmap(mPath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 75, bos);
            if (bm != null && !bm.isRecycled()) {
                bm.recycle();
                bm = null;
            }

            ContentBody contentPart = new ByteArrayBody(bos.toByteArray(), mPath);
            mReqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            mReqEntity.addPart("image", contentPart);
            try {
                mReqEntity.addPart("st", new StringBody(URLEncoder.encode("mobilelens", "UTF-8")));
                mReqEntity.addPart("sm", new StringBody(URLEncoder.encode("nostate", "UTF-8")));
                mReqEntity.addPart("imgnum", new StringBody(URLEncoder.encode("60", "UTF-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }


        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... voids) {
            long time = System.currentTimeMillis();
            try {
                URL url = new URL(mEncryptedURL);
                mConnection = (HttpURLConnection) url.openConnection();
                mConnection.setReadTimeout(7000);
                mConnection.setConnectTimeout(10000);
                mConnection.setRequestMethod("POST");
                mConnection.setUseCaches(false);
                mConnection.setDoInput(true);
                mConnection.setDoOutput(true);

                mConnection.setRequestProperty("Connection", "Keep-Alive");
                mConnection.setFixedLengthStreamingMode(mReqEntity.getContentLength());
                mConnection.addRequestProperty(mReqEntity.getContentType().getName(), mReqEntity.getContentType().getValue());
                OutputStream os = mConnection.getOutputStream();
                mReqEntity.writeTo(os);
                os.close();
                if (checkCancel()) {
                    this.cancel(true);
                    return null;
                }

                mConnection.connect();
                Log.d(TAG, "conn.getResponseCode() = " + mConnection.getResponseCode());
                if (mConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "response time = " + (System.currentTimeMillis() - time));
                    if (checkCancel()) {
                        this.cancel(true);
                        Log.d(TAG, "return null");
                        return null;
                    }
                    response = NaverLensXmlParser.readStream(mConnection.getInputStream());
                    Log.d(TAG, "response = " + response);
                }
            } catch (Exception e) {
                Log.e(TAG, "multipart post error " + e.toString());
                mCB.onNaverLensRequestCanceled(-1);
            } finally {
                Log.d(TAG, "Disconnecting server");
                if (mConnection != null) {
                    mConnection.disconnect();
                    mConnection = null;
                }
            }
            return null;
        }

        protected void onPostExecute(Void aVoid) {
            Log.d(TAG, "PostExecute");
            super.onPostExecute(aVoid);
            mCB.onNaverLensCompleted(response);
        }

        protected void onCancelled(Void aVoid) {
            Log.d(TAG, "onCancelled aVoid");
            mCB.onNaverLensRequestCanceled(-1);
            super.onCancelled(aVoid);
        }
    }

    public void getNaverInformation() {
        // FOR TEST
        Log.d(TAG, "Naver Smart Lens API");
    }

}
