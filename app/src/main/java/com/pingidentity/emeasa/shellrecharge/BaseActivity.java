package com.pingidentity.emeasa.shellrecharge;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    public abstract void onFingerPrintAuthenticatedCallback(String pairingKey);
    public abstract void onFingerPrintAuthenticatedCallback(boolean cancelled);
}
