package com.pingidentity.emeasa.shellrecharge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

public class ShellRechargeMobileApplication extends Application {

    private static final String PAIRED = "paired";
    private static String TAG = "com.pingidentity.emeasa.shellrecharge.ShellRechargeMobileApplication";



    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        Log.d(TAG, token);
                        //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                        PingOne.setDeviceToken(ShellRechargeMobileApplication.this, token, new PingOne.PingOneSDKCallback() {
                            @Override
                            public void onComplete(@Nullable PingOneSDKError error) {
                                Log.d(TAG, "set Device Token");
                            }
                        });
                    }
                });
    }


    public  String getPreference(Context context, String preferenceName) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return _sharedPrefs.getString(preferenceName, "");
    }

    public  boolean isPaired(Context context) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return _sharedPrefs.getBoolean(PAIRED, false);
    }


    public  void setPreference(Context context, String preferenceName, String text) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor _prefsEditor;
        _prefsEditor = _sharedPrefs.edit();
        _prefsEditor.putString(preferenceName, text);
        _prefsEditor.apply();
    }

    public void setPaired(Context context, boolean b) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor _prefsEditor;
        _prefsEditor = _sharedPrefs.edit();
        _prefsEditor.putBoolean(PAIRED, b);
        _prefsEditor.apply();
    }

    public int getLoginState(Context context) {
        if (getPreference(context, EMAIL) == null)
            return MUST_REGISTER;
        if (getPreference(context, ACCESS_TOKEN) == null && getPreference(context, REFRESH_TOKEN) == null)
            return MUST_LOG_IN;
        return USE_LOCAL_BIO;
    }

    public static String EMAIL = "email";
    public static String ID_TOKEN = "id_token";
    public static String ACCESS_TOKEN = "access_token";
    public static String REFRESH_TOKEN = "refresh_token";
    public static int MUST_REGISTER = 0;
    public static int MUST_LOG_IN = 1;
    public static int USE_LOCAL_BIO = 2;


}
