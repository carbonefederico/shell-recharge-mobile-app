package com.pingidentity.emeasa.shellrecharge;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;


import org.json.JSONException;
import org.json.JSONObject;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
        PingOne.processRemoteNotification(MessagingService.this, remoteMessage, new PingOne.PingOneNotificationCallback() {
            @Override
            public void onComplete(@Nullable NotificationObject pingOneNotificationObject, PingOneSDKError error) {
                if (pingOneNotificationObject == null){
                    //the push is not from PingOne  - handle it your way
                }else if(!pingOneNotificationObject.isTest()){


                    //the object contains two options - approve and deny - present them to the user
                    Intent handleNotificationObjectIntent = new Intent(MessagingService.this, ApprovalActivity.class);
                    handleNotificationObjectIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    handleNotificationObjectIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    parseTitleAndBody(remoteMessage, handleNotificationObjectIntent);
                    handleNotificationObjectIntent.putExtra("PingOneNotification", pingOneNotificationObject);

                    /*
                     * if application is in foreground process the push in activity
                     */
                    if(ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)){
                        startActivity(handleNotificationObjectIntent);




                        /*pingOneNotificationObject.approve(MessagingService.this, "user", new PingOne.PingOneSDKCallback() {
                            @Override
                            public void onComplete(@Nullable final PingOneSDKError pingOneSDKError) {

                            }
                        }); */


                    }else {
                        /*
                         * if the application is in the background build and show Notification
                         */
                        NotificationsManager notificationsManager = new NotificationsManager(MessagingService.this);
                        notificationsManager.buildAndSendNotification(handleNotificationObjectIntent);
                    }


                }
            }
        });

    }

    /**
     * Parse the "aps" part of the RemoteMessage to get notifications' title and body
     * @param remoteMessage
     * @param intent
     */
    private void parseTitleAndBody(RemoteMessage remoteMessage, Intent intent){
        if(remoteMessage.getData().containsKey("aps")){
            try {
                JSONObject jsonObject = new JSONObject(remoteMessage.getData().get("aps"));
                intent.putExtra("title", ((JSONObject)jsonObject.get("alert")).get("title").toString());
                intent.putExtra("body", ((JSONObject)jsonObject.get("alert")).get("body").toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        PingOne.setDeviceToken(this, token, new PingOne.PingOneSDKCallback() {
            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                //check for an error and re-schedule service update
            }
        });
        saveFcmRegistrationToken(token);
    }

    private void saveFcmRegistrationToken(@NonNull String token){
        SharedPreferences sharedPreferences = getSharedPreferences("InternalPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pushToken", token);
        editor.apply();
    }
}
