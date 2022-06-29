package com.pingidentity.emeasa.shellrecharge;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;


import androidx.annotation.Nullable;


import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.pingidentity.emeasa.shellrecharge.databinding.ActivityApprovalBinding;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

import org.json.JSONException;
import org.json.JSONObject;

public class ApprovalActivity extends BaseActivity {


    private ActivityApprovalBinding binding;
    FingerprintAuthenticationDialogFragment fragment;
    private static final String FINGERPRINT_AUTH_DIALOG_TAG = "fingerprint_authentication_fragment";
    private NotificationObject pingOneNotificationObject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApprovalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if(getIntent().hasExtra("PingOneNotification")){
            NotificationObject pingOneNotificationObject = (NotificationObject) getIntent().getExtras().get("PingOneNotification");
            String device = "Unkown";
            String location = "Unknown";
            try {
                JSONObject clientContext = new JSONObject(pingOneNotificationObject.getClientContext());
                device = clientContext.getString("device");
                location = clientContext.getString("location");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String title = "Authenticate?";
            String body = null;
            if(getIntent().hasExtra("title")){
                title = getIntent().getStringExtra("title");
            }
            if (getIntent().hasExtra("body")){
                body = getIntent().getStringExtra("body");
            }
            ((TextView) findViewById(R.id.lblTitle)).setText(title);
            ((TextView) findViewById(R.id.lblDescription)).setText(body);
            String details = String.format("Sent from %s near %s", device, location);
            ((TextView) findViewById(R.id.lblDeviceDetails)).setText(details);
            Button approve = (Button) findViewById(R.id.buttonApprove);
            Button reject = (Button) findViewById(R.id.buttonReject);
            reject.setOnClickListener(new View.OnClickListener() {


                @Override
                public void onClick(View v) {
                    pingOneNotificationObject.deny(ApprovalActivity.this, new PingOne.PingOneSDKCallback() {
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                           finish();
                        }
                    });
                }
            });

            approve.setOnClickListener(new View.OnClickListener() {


                @Override
                public void onClick(View v) {
                   ApprovalActivity.this.pingOneNotificationObject = pingOneNotificationObject;
                   triggerBiometricApproval("Confirm Approval" , "Please approve this purchase");
                }
            });
        }





    }

    protected void triggerBiometricApproval(String title, String description) {
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
        System.out.println(fingerprintManager.isHardwareDetected());
        System.out.println(fingerprintManager.hasEnrolledFingerprints());
        if(fingerprintManager!=null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()){
            fragment = new FingerprintAuthenticationDialogFragment();
            Bundle b = new Bundle();
            b.putString("title", title);
            b.putString("description", description);

            fragment.setArguments(b);
            fragment.show(getFragmentManager(), FINGERPRINT_AUTH_DIALOG_TAG);
        }
    }

    public void onFingerPrintAuthenticatedCallback(String pairingKey) {

        pingOneNotificationObject.approve(ApprovalActivity.this, new PingOne.PingOneSDKCallback() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                fragment.dismissAllowingStateLoss();
                finish();
            }
        });


    }

    @Override
    public void onFingerPrintAuthenticatedCallback(boolean cancelled) {

    }

}