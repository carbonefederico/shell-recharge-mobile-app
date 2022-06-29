package com.pingidentity.emeasa.shellrecharge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.text.Html;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.navigation.ui.AppBarConfiguration;


import com.pingidentity.emeasa.shellrecharge.databinding.ActivityMainBinding;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.types.PairingInfo;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class MainActivity extends BaseActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static String TAG = "com.pingidentity.emeasa.shellrecharge.MainActivity";
    private int biomode=0;
    private String challenge = null;

    private DaVinciHelper helper;

    private static final String FINGERPRINT_AUTH_DIALOG_TAG = "fingerprint_authentication_fragment";
    FingerprintAuthenticationDialogFragment fragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
        if (appLinkData != null)
         challenge = appLinkData.getQueryParameter("c");
        helper = new DaVinciHelper();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        ShellRechargeMobileApplication app = (ShellRechargeMobileApplication) getApplication();
      /*  if (app.isPaired(this)) {
            doFingerprintLogin();
        } else {

            updateView(3);
        } */

        updateView(MODE_NOTHING);

        Button acctButton = (Button) this.findViewById(R.id.acctButton);
        acctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateView(0);
            }
        });


        Button registerButton = (Button) this.findViewById(R.id.buttonRegister);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                try {
                    JSONObject flowInput = new JSONObject();
                   // flowInput.put("mobilePayload", PingOne.generateMobilePayload(MainActivity.this));
                    helper.startFlow(MainActivity.this, flowInput, "652b82eeac2effe1ac369644993310a3");
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        Button buttonSignin = (Button) this.findViewById(R.id.buttonSignin);
        buttonSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // here we will check to see if the app is paired
                if (app.isPaired(MainActivity.this)) {
                    MainActivity.this.doFingerprintLogin();
                } else {
                    try {
                        JSONObject flowInput = new JSONObject();
                        // flowInput.put("mobilePayload", PingOne.generateMobilePayload(MainActivity.this));
                        helper.startFlow(MainActivity.this, flowInput, "9fd5ac6e1de508133353f5b95ef97767");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Button nextButton = (Button) this.findViewById(R.id.buttonNext);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                try {
                    hideKeyboard(MainActivity.this);
                    Object[] formInputs = new Object[5];
                    EditText input1 = (EditText) findViewById(R.id.input1);
                    formInputs[0] = input1.getText().toString().trim();
                    EditText input2 = (EditText) findViewById(R.id.input2);
                    formInputs[1] = input2.getText().toString().trim();
                    EditText input3 = (EditText) findViewById(R.id.input3);
                    formInputs[2] = input3.getText().toString().trim();
                    CheckBox checkbox4 = (CheckBox) findViewById(R.id.checkBox4);
                    formInputs[3] = checkbox4.isChecked();

                    helper.continueFlow(MainActivity.this, formInputs);
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

    }

    private void doFingerprintLogin() {
        triggerBiometricAuth("Fingerprint Login", "Please touch the sensor to login", 1, null);


    }


    private void updateView(int mode){
        Button acctButton = (Button) this.findViewById(R.id.acctButton);
        if (mode == MODE_NOTHING) {

            acctButton.setVisibility(View.VISIBLE);
        } else {
            acctButton.setVisibility(View.GONE);
        }
        findViewById(R.id.layout_initial).setVisibility(View.GONE);
        findViewById(R.id.layout_start).setVisibility(View.GONE);
        findViewById(R.id.layout_loggedin).setVisibility(View.GONE);
        if (mode == MODE_INITIAL)
            findViewById(R.id.layout_initial).setVisibility(View.VISIBLE);
        else if (mode == MODE_START)
            findViewById(R.id.layout_start).setVisibility(View.VISIBLE);
        else if (mode == MODE_LOGGED_IN)
            findViewById(R.id.layout_loggedin).setVisibility(View.VISIBLE);
        findViewById(R.id.lblPaired).setVisibility(View.GONE);
    }

    private static int MODE_INITIAL=0;
    private static int MODE_START=1;
    private static int MODE_LOGGED_IN=2;
    private static int MODE_NOTHING=3;

    public void updateFlowUI(JSONObject screen) throws JSONException {

        updateFlowUI(screen, false);
    }
    public void updateFlowUI(JSONObject screen, boolean polling) throws JSONException {
        JSONObject properties = screen.getJSONObject("properties");
        int[] inputFieldIDs = {R.id.input1, R.id.input2,R.id.input3, R.id.checkBox4};
        int[] inputLabelIDs = {R.id.lbl1, R.id.lbl2,R.id.lbl3, R.id.lbl4};

        // Hide everything
        for (int i = 0; i < 4; i++) {
            findViewById(inputFieldIDs[i]).setVisibility(View.GONE);
            if (findViewById(inputFieldIDs[i]) instanceof EditText)
                ((EditText)findViewById(inputFieldIDs[i])).setText("");
            findViewById(inputLabelIDs[i]).setVisibility(View.GONE);
        }
        if (polling) {
            ((TextView) findViewById(R.id.lblTitle)).setText(properties.getJSONObject("messageTitle").getString("value"));
          //  ((TextView) findViewById(R.id.lblDescription)).setText(properties.getJSONObject("message").getString("value"));
         //   findViewById(R.id.lblDescription).setVisibility(View.VISIBLE);
        //    ((TextView) findViewById(R.id.lblDescription)).setMovementMethod( null);

            JSONArray lines = new JSONArray(properties.getJSONObject("message").getString("value"));

            for (int i =0; i < lines.length(); i++) {
                JSONObject line = lines.getJSONObject(i);
                findViewById(inputLabelIDs[i]).setVisibility(View.VISIBLE);
                ((TextView) findViewById(inputLabelIDs[i])).setText(Html.fromHtml(line.getString("line"),0));

                findViewById(R.id.buttonNext).setVisibility(View.GONE);
            }
            int pollInterval = properties.getJSONObject("pollInterval").getInt("value");
            helper.doPoll(this,pollInterval );


        } else {

            ((TextView) findViewById(R.id.lblTitle)).setText(properties.getJSONObject("title").getString("value"));
            if (properties.getJSONObject("bodyHeaderText").has("value"))
                ((TextView) findViewById(R.id.lblDescription)).setText(properties.getJSONObject("bodyHeaderText").getString("value"));
            findViewById(R.id.lblDescription).setVisibility(View.GONE);



            JSONArray inputFields = properties.getJSONObject("formFieldsList").getJSONArray("value");
            for (int i =0; i < inputFields.length(); i++) {
                JSONObject inputField = inputFields.getJSONObject(i);
                if (inputField.getString("preferredControlType").equals("textField")) {
                    findViewById(inputFieldIDs[i]).setVisibility(View.VISIBLE);
                    findViewById(inputLabelIDs[i]).setVisibility(View.VISIBLE);
                    TextView label = (TextView) findViewById(inputLabelIDs[i]);
                    EditText input = (EditText) findViewById(inputFieldIDs[i]);
                    boolean secret = inputField.getBoolean("hashedVisibility");
                    if (secret)
                        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    else input.setTransformationMethod(null);
                    label.setText(inputField.getString("displayName"));
                    input.setText(inputField.getString("value"));
                } else if (inputField.getString("preferredControlType").equals("label")) {
                    findViewById(inputFieldIDs[i]).setVisibility(View.GONE);
                    findViewById(inputLabelIDs[i]).setVisibility(View.VISIBLE);
                    TextView label = (TextView) findViewById(inputLabelIDs[i]);
                    label.setText(inputField.getString("displayName"));
                } else if (inputField.getString("preferredControlType").equals("toggleSwitch")) {
                    findViewById(inputFieldIDs[i]).setVisibility(View.VISIBLE);
                    findViewById(inputLabelIDs[i]).setVisibility(View.VISIBLE);
                    TextView label = (TextView) findViewById(inputLabelIDs[i]);
                    label.setText(Html.fromHtml(inputField.getString("displayName"),0));
                }


            }
            ((EditText) findViewById(R.id.input1)).requestFocus();
            ((Button) findViewById(R.id.buttonNext)).setText(properties.getJSONObject("nextButtonText").getString("value"));
            findViewById(R.id.buttonNext).setVisibility(View.VISIBLE);
            updateView(1);
        }
    }

    public void updateFlowUIWithMessage(JSONObject screen) throws JSONException {
        JSONObject properties = screen.getJSONObject("properties");
        ((TextView) findViewById(R.id.lblTitle)).setText(properties.getJSONObject("messageTitle").getString("value"));
        ((TextView) findViewById(R.id.lblDescription)).setText(properties.getJSONObject("message").getString("value"));
        ((TextView) findViewById(R.id.lblDescription)).setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.input1).setVisibility(View.GONE);
        findViewById(R.id.input2).setVisibility(View.GONE);
        ((EditText)findViewById(R.id.input1)).setText("");
        ((EditText)findViewById(R.id.input2)).setText("");
        findViewById(R.id.buttonNext).setVisibility(View.VISIBLE);
        ((Button) findViewById(R.id.buttonNext)).setText(properties.getJSONObject("button").getString("displayName"));
    }

    public void showRegistrationSuccess() {
        findViewById(R.id.lblPaired).setVisibility(View.VISIBLE);
    }

    public void setEmailAndTokens(String email, String access_token, String id_token) {
       ShellRechargeMobileApplication app = (ShellRechargeMobileApplication)getApplication();
       app.setPreference(this, ShellRechargeMobileApplication.EMAIL, email);
        app.setPreference(this, ShellRechargeMobileApplication.ACCESS_TOKEN, access_token);
        app.setPreference(this, ShellRechargeMobileApplication.ID_TOKEN, id_token);
        String newUser = "";
        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();
        try {
            JwtContext jwtContext = firstPassJwtConsumer.process(id_token);
            String name = jwtContext.getJwtClaims().getClaimValueAsString("name");
            ((TextView) findViewById(R.id.lblUser)).setText(String.format("%s%s", getGreeting(), name));
             newUser  = jwtContext.getJwtClaims().getClaimValueAsString("newUser");
            if ("true".equalsIgnoreCase(newUser)) {
                showRegistrationSuccess();
                app.setPaired(this, false);
            }
            String pairingKey  = jwtContext.getJwtClaims().getClaimValueAsString("pairingKey");
            if (pairingKey != null && pairingKey.length() > 0) {
                // Set up auto-login
                triggerBiometricAuth("Passwordless Login", "You can quickly and easily log in the next time using this device's fingerprint or face recognition", 0, pairingKey);
            }
        } catch (InvalidJwtException e) {
            e.printStackTrace();
        }

        //String email = app.getPreference(this, ShellRechargeMobileApplication.EMAIL);


       /* if (!app.isPaired(this)) {
            triggerBiometricAuth("Fingerprint Login", "Please touch the sensor to set up fingerprint auto-login", 0);
        } else {
            updateView(MODE_LOGGED_IN);
        }*/
        if ("true".equalsIgnoreCase(newUser)) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    updateView(MODE_LOGGED_IN);
                }
            };
            handler.postDelayed(r, 3000);
        } else updateView(MODE_LOGGED_IN);



    }

    public void onFingerPrintAuthenticatedCallback(String pairingKey) {
        fragment.dismissAllowingStateLoss();
        if (biomode==0) {
            ShellRechargeMobileApplication app = (ShellRechargeMobileApplication) getApplication();
            PingOne.pair(this, pairingKey, new PingOne.PingOneSDKPairingCallback() {
                @Override
                public void onComplete(PairingInfo pairingInfo, @Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                    app.setPaired(MainActivity.this, true);
                }
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {

                }
            });

        } else if (biomode == 1) {

            try {
                ShellRechargeMobileApplication app = (ShellRechargeMobileApplication) getApplication();
                JSONObject flowInput = new JSONObject();
                flowInput.put("mobilePayload", PingOne.generateMobilePayload(MainActivity.this));
                flowInput.put("email", app.getPreference(this, ShellRechargeMobileApplication.EMAIL));
                if (challenge != null) {
                    flowInput.put("challenge", challenge);
                }
                helper.startFlow(MainActivity.this, flowInput, "747614899c9c3cdcbf87e1a6bc3875d4");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onFingerPrintAuthenticatedCallback(boolean cancelled) {
        fragment.dismissAllowingStateLoss();
        if (biomode == 1) {

            try {
                ShellRechargeMobileApplication app = (ShellRechargeMobileApplication) getApplication();
                JSONObject flowInput = new JSONObject();
                helper.startFlow(MainActivity.this, flowInput, "9fd5ac6e1de508133353f5b95ef97767");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected boolean bioPromptAvailable () {
        boolean result = true;
        result = result && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);
        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(this);
        result = result &&  fingerprintManager.isHardwareDetected();
        result = result && fingerprintManager.hasEnrolledFingerprints();
        return result && (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) ==
                PackageManager.PERMISSION_GRANTED);
    }

    protected boolean isPaired() {
        SharedPreferences prefs = this.getSharedPreferences("PAIRING", MODE_PRIVATE );
        return prefs.getBoolean("PAIRED", false);
    }

    protected void triggerBiometricAuth(String title, String description, int mode, String pairingKey) {
        biomode = mode;
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
        System.out.println(fingerprintManager.isHardwareDetected());
        System.out.println(fingerprintManager.hasEnrolledFingerprints());
        if(fingerprintManager!=null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()){
            fragment = new FingerprintAuthenticationDialogFragment();
            Bundle b = new Bundle();
            b.putString("title", title);
            b.putString("description", description);
            if (pairingKey != null) {
                b.putString("pairingKey", pairingKey);
            }
            if (mode == 0) {
                b.putString("cancelText", "Maybe later" );
            } else if (mode == 1) {
                b.putString("cancelText", "Login with Password");
            }

            fragment.setArguments(b);
            fragment.show(getFragmentManager(), FINGERPRINT_AUTH_DIALOG_TAG);
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void doLogout(View view) {
        ShellRechargeMobileApplication app = (ShellRechargeMobileApplication)getApplication();

        app.setPreference(this, ShellRechargeMobileApplication.ACCESS_TOKEN, null);
        app.setPreference(this, ShellRechargeMobileApplication.ID_TOKEN, null);
        ((TextView) findViewById(R.id.lblUser)).setText("");

        updateView(MODE_NOTHING);
    }
    public String getGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if(timeOfDay >= 0 && timeOfDay < 12){
            return("Good morning, ");
        }else if(timeOfDay >= 12 && timeOfDay < 16){
            return("Good afternoon, ");
        }else if(timeOfDay >= 16 && timeOfDay < 21){
            return("Good evening, ");
        }else if(timeOfDay >= 21 && timeOfDay < 24){
            return("Good night, ");
        }
        return "Hello, ";
    }
}