package com.soa.app.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class NetworkConnectivity {

    private final AppExecutors appExecutors;
    private final Context context;

    public NetworkConnectivity(AppExecutors appExecutors, Context context) {
        this.appExecutors = appExecutors;
        this.context = context;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (cap == null) return false;
            return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = cm.getAllNetworks();
            for (Network n: networks) {
                NetworkInfo nInfo = cm.getNetworkInfo(n);
                if (nInfo != null && nInfo.isConnected()) return true;
            }
        } else {
            NetworkInfo[] networks = cm.getAllNetworkInfo();
            for (NetworkInfo nInfo: networks) {
                if (nInfo != null && nInfo.isConnected()) return true;
            }
        }

        return false;
    }

    public synchronized void checkInternetConnection(ConnectivityCallback callback) {
        appExecutors.getNetworkIO().execute(() -> {
            if (isNetworkAvailable()) {
                try {
                    HttpsURLConnection urlc = (HttpsURLConnection)
                            new URL("https://clients3.google.com/generate_204").openConnection();
                    urlc.setRequestProperty("User-Agent", "Android");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(1000);
                    urlc.connect();
                    boolean isConnected = urlc.getResponseCode() == 204 && urlc.getContentLength() == 0;
                    postCallback(callback, isConnected);
                } catch (Exception e) {
                    postCallback(callback, false);
                }
            } else {
                postCallback(callback, false);
            }
        });
    }

    public interface ConnectivityCallback {
        void onDetected(boolean isConnected);
    }

    private void postCallback(ConnectivityCallback callBack, boolean isConnected) {
        appExecutors.mainThread().execute(() -> callBack.onDetected(isConnected));
    }

}
