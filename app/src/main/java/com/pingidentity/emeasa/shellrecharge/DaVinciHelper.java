package com.pingidentity.emeasa.shellrecharge;


import androidx.annotation.Nullable;

import com.loopj.android.http.AsyncHttpClient;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.pingidentity.pingidsdkv2.PairingObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.types.PairingInfo;

import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class DaVinciHelper {


    private String flowInitiationToken;
    private MainActivity callingActivity;
    private JSONObject flowInput;
    private String responseId;
    private String interactionId;
    private String interactionToken;
    private JSONObject screen;
    private JSONObject nextPayload;
    private String nextURL;
    private JSONObject pollPayload;
    private String pollURL;

    public void startFlow(MainActivity activity, JSONObject input, String policyID) {
        callingActivity = activity;
        flowInput = input;
        if (flowInitiationToken == null) {
            fetchFlowInitiationToken(policyID);
        } else {
            startFlowWithToken(policyID);
        }
    }

    public void startFlowWithToken(String policyID) {
        // Here we can be sure that the token is not null
        String requestURL = String.format("%sauth/%s/policy/%s/start", DV_API_URL_BASE, COMPANY_ID, policyID);
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("AUTHORIZATION", String.format("Bearer %s", this.flowInitiationToken));
        StringEntity entity = new StringEntity(this.flowInput.toString(), "UTF-8");
        client.post(callingActivity, requestURL, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    responseId = response.getString("id");
                    if (response.has("interactionId"))
                        interactionId = response.getString("interactionId");
                    if (response.has("interactionToken"))
                        interactionToken = response.getString("interactionToken");
                    if (response.has("screen"))
                        screen = response.getJSONObject("screen");
                    String connectionId = response.getString("connectionId");
                    String capabilityName = response.getString("capabilityName");
                    if (capabilityName.equalsIgnoreCase("createSessionWithCustomClaims")) {
                        String access_token = response.getString("access_token");
                        String id_token = response.getString("id_token");
                        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                                .setSkipAllValidators()
                                .setDisableRequireSignature()
                                .setSkipSignatureVerification()
                                .build();
                        JwtContext jwtContext = firstPassJwtConsumer.process(id_token);
                        String email = jwtContext.getJwtClaims().getClaimValueAsString("email");
                        callingActivity.setEmailAndTokens(email, access_token, id_token);
                        doCleanUp();
                    } else {
                        nextPayload = new JSONObject();
                        nextPayload.put("nextEvent", screen.getJSONObject("properties").getJSONObject("nextEvent"));
                        nextPayload.put("eventName", "submitFormInput");
                        nextPayload.put("connectionId", connectionId);
                        nextPayload.put("capabilityName", capabilityName);
                        nextPayload.put("userViewIndex", 0);
                        nextPayload.put("id", responseId);

                        nextURL = String.format("%sauth/%s/connections/%s/capabilities/%s", DV_API_URL_BASE, COMPANY_ID, connectionId, capabilityName);
                        callingActivity.updateFlowUI(screen);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                try {
                    t.printStackTrace();
                    doCleanUp();

                } catch (Exception e) {
                    e.printStackTrace();

                }
            }

        });
    }

    public void continueFlow(MainActivity mainActivity, Object[] formInputs) throws JSONException {
        JSONObject flowInput = new JSONObject();
       /* for (int i = 0; i < formInputs.length; i++) {
            Object inputValue = formInputs[i];
            if ( inputValue instanceof String && inputValue != null && ((String)inputValue).length() >0) {
                String paramName = screen.getJSONObject("properties").getJSONObject("formFieldsList").getJSONArray("value").getJSONObject(i).getString("propertyName");
                flowInput.put(paramName, (String) inputValue);
            }
            if (inputValue instanceof Boolean && screen.getJSONObject("properties").getJSONObject("formFieldsList").getJSONArray("value").length() > i) {
                //Kill me for writing the above line of code
                String paramName = screen.getJSONObject("properties").getJSONObject("formFieldsList").getJSONArray("value").getJSONObject(i).getString("propertyName");
                flowInput.put(paramName, (Boolean) inputValue);
            }
        }*/
        if (screen.getJSONObject("properties").has("formFieldsList")) {
            JSONArray fieldList = screen.getJSONObject("properties").getJSONObject("formFieldsList").getJSONArray("value");
            for (int i = 0; i < fieldList.length(); i++) {
                JSONObject field = fieldList.getJSONObject(i);
                Object inputValue = formInputs[i];

                if (inputValue instanceof String && ((String) inputValue).length() == 0) {
                    flowInput.put(field.getString("propertyName"), "XX");
                } else {
                    flowInput.put(field.getString("propertyName"), formInputs[i]);
                }
            }
        }
        if (flowInput.keys().hasNext()) {
            nextPayload.put("parameters", flowInput);
        }
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("AUTHORIZATION", String.format("Bearer %s", this.flowInitiationToken));
        client.addHeader("interactionId", interactionId);
        client.addHeader("interactionToken", interactionToken);
        StringEntity entity = new StringEntity(nextPayload.toString(), "UTF-8");
        client.post(callingActivity, nextURL, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    responseId = response.getString("id");
                    if (response.has("interactionId"))
                        interactionId = response.getString("interactionId");
                    if (response.has("interactionToken"))
                        interactionToken = response.getString("interactionToken");
                    if (response.has("screen"))
                        screen = response.getJSONObject("screen");
                    String connectionId = response.getString("connectionId");
                    String capabilityName = response.getString("capabilityName");
                    nextPayload = new JSONObject();
                    nextURL = String.format("%sauth/%s/connections/%s/capabilities/%s", DV_API_URL_BASE, COMPANY_ID, connectionId, capabilityName);
                    nextPayload.put("id", responseId);
                    if (capabilityName.equalsIgnoreCase("htmlFormInput")) {

                        nextPayload.put("nextEvent", screen.getJSONObject("properties").getJSONObject("nextEvent"));
                        nextPayload.put("eventName", "submitFormInput");
                        nextPayload.put("connectionId", connectionId);
                        nextPayload.put("capabilityName", capabilityName);
                        nextPayload.put("userViewIndex", 0);
                        callingActivity.updateFlowUI(screen);
                    } else if (capabilityName.equalsIgnoreCase("customHtmlMessage")) {

                        if (screen.getJSONObject("properties").getJSONObject("enablePolling").getBoolean("value")) {
                            //We need to set up challenge polling
                            pollPayload = new JSONObject();
                            pollPayload.put("eventName", "polling");
                            pollPayload.put("connectionId", connectionId);
                            pollPayload.put("capabilityName", capabilityName);
                            pollPayload.put("flowId", FLOW_ID);
                            pollPayload.put("id", responseId);
                            String challenge = screen.getJSONObject("properties").getJSONObject("challenge").getString("value");
                            pollURL = String.format("%sauth/%s/user/credentials/challenge/%s/status", DV_API_URL_BASE, COMPANY_ID, challenge);
                            callingActivity.updateFlowUI(screen, true);
                        } else {
                            //God this is such a hack. I hate myself
                            String msgTitle = screen.getJSONObject("properties").getJSONObject("messageTitle").getString("value");

                            if("pairing".equalsIgnoreCase(msgTitle)) {
                                nextPayload.put("eventName", "continue");
                                JSONObject messagePayload = new JSONObject( screen.getJSONObject("properties").getJSONObject("message").getString("value"));
                                String pairingKey = messagePayload.getString("pairingKey");
                                PingOne.pair(callingActivity, pairingKey, new PingOne.PingOneSDKPairingCallback() {
                                    @Override
                                    public void onComplete(PairingInfo pairingInfo, @Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {


                                    }

                                    @Override
                                    public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {

                                    }
                                });

                                try {
                                    continueFlow(mainActivity, new String[5]);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                //This is a standard message to display and move on
                                nextPayload.put("eventName", "continue");
                                callingActivity.updateFlowUIWithMessage(screen);
                            }
                        }
                    }else if (capabilityName.equalsIgnoreCase("createSessionWithCustomClaims")) {
                        String access_token = response.getString("access_token");
                        String id_token = response.getString("id_token");
                        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                                .setSkipAllValidators()
                                .setDisableRequireSignature()
                                .setSkipSignatureVerification()
                                .build();
                        JwtContext jwtContext = firstPassJwtConsumer.process(id_token);
                        String email = jwtContext.getJwtClaims().getClaimValueAsString("email");
                        mainActivity.setEmailAndTokens(email, access_token, id_token);
                        doCleanUp();

                    }
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                try {
                    t.printStackTrace();
                    doCleanUp();

                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });


    }

    public void doPoll(MainActivity mainActivity, int pollInterval) {
        try {
            Thread.sleep(pollInterval);
        } catch (Exception e) {
            e.printStackTrace();
        }
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("AUTHORIZATION", String.format("Bearer %s", this.flowInitiationToken));
        client.addHeader("interactionId", interactionId);
        client.addHeader("interactionToken", interactionToken);
        StringEntity entity = new StringEntity(pollPayload.toString(), "UTF-8");
        client.post(callingActivity, pollURL, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String status = response.getString("status");
                    if ("approved".equalsIgnoreCase(status)) {
                        nextPayload.put("eventName", "continue");
                        JSONObject pollResponse = new JSONObject();
                        pollResponse.put("status", status);
                        nextPayload.put("pollResponse", pollResponse);
                        pollPayload = null;
                        pollURL = null;
                        continueFlow(mainActivity, new String[5]);
                    } else {
                        doPoll(mainActivity, pollInterval);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }

        });
    }

    private void fetchFlowInitiationToken(String policyID) {
        try {
            String requestURL = String.format("%scompany/%s/sdkToken", DV_API_URL_BASE, COMPANY_ID);
            AsyncHttpClient client = new AsyncHttpClient();
                client.addHeader("X-SK-API-KEY", API_KEY);
            client.get(requestURL, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        flowInitiationToken = response.getString("access_token");
                        startFlowWithToken(policyID);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doCleanUp() {
        this.interactionId = null;
        this.interactionToken = null;
        this.responseId = null;
        this.flowInput = null;
        this.screen = null;
        this.nextPayload = null;
        this.nextURL = null;
        this.pollPayload = null;
        this.pollURL = null;
    }

    private static String COMPANY_ID = "cbe83512-3062-4ec9-bb35-1b6abc351173";
    private static  String FLOW_ID = "FIX";
    private static String API_KEY = "94d9c563e0a1812a21ea63da4550bf43df22297d073a1bfb632dd7ce6bbd597fd122e4ab190d48310570c6c1b074f30a1c6263b06239e913c74d9abfb9110c9e32dafc1c4b54177833fc4380a6a2d9e93851c43406a9a69df668b6a9f672a7207cfb15c9131dc576aa10a673e8cef51a73a776ed337aff3732b000fcf8f03612";
    private static String DV_API_URL_BASE = "https://api.singularkey.com/v1/";


}
