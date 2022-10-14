package de.niklasmerz.cordova.biometric;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.biometric.BiometricManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;

public class Fingerprint extends CordovaPlugin {
    private static final String TAG = "Fingerprint";
    private static final int REQUEST_CODE_BIOMETRIC = 1;

    private String applicationLabel;
    private CallbackContext mCallbackContext = null;

    private CryptographyManager mCryptographyManager;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.v(TAG, "Init Fingerprint");

        applicationLabel = getApplicationLabel(cordova.getActivity());

        mCryptographyManager = new CryptographyManagerImpl();
    }

    public boolean execute(final String action, JSONArray args, CallbackContext callbackContext) {
        this.mCallbackContext = callbackContext;
        Log.v(TAG, "Fingerprint action: " + action);

        // All methods require access to security checks
        PluginError error = canAuthenticate();
        if (error != null) {
            sendError(error);
            return true;
        }

        // All other methods perform a challenge and/or action, with shared args
        BiometricActivityType type = BiometricActivityType.fromJsonString(action);
        if (type == null) {
            // Unknown action
            return false;
        }

        PromptInfo parsedArgs = new PromptInfo.Builder(applicationLabel)
            .parseArgs(args, type)
            .build();

        switch(type) {
            case IS_AVAILABLE:
                // canAuthenticate above already did the check
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    sendSuccess("biometric");
                } else {
                    sendSuccess("biometric+passcode");
                }
                return true;
            case HAS_SECRET:
                // Checking for existence is always non-interactive
                hasSecret(parsedArgs);
                return true;
            case SET_SECRET:
                if (parsedArgs.getSecret() == null) {
                    sendError(PluginError.BIOMETRIC_ARGS_PARSING_FAILED);
                    return true;
                }
                break;
        }

        runBiometricActivity(parsedArgs);
        return true;
    }

    private PluginError canAuthenticate() {
        int error;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            error = BiometricManager.from(cordova.getContext())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        } else {
            error = BiometricManager.from(cordova.getContext())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        }
        switch (error) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return null;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return PluginError.BIOMETRIC_NOT_ENROLLED;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            default:
                return PluginError.BIOMETRIC_HARDWARE_NOT_SUPPORTED;
        }
    }

    private void hasSecret(PromptInfo promptInfo) {
        try {
            byte[] initializationVector = EncryptedData
                .loadInitializationVector(
                        promptInfo.getSecretName(),
                        cordova.getActivity().getApplicationContext()
                );
            Cipher cipher = mCryptographyManager
                .getInitializedCipherForDecryption(promptInfo.getSecretName(), initializationVector);
            sendSuccess(true);
        } catch (CryptoException e) {
            if (e.getCause() instanceof UserNotAuthenticatedException) {
                sendSuccess(true);
            }
            sendError(e.getError());
        } catch (Exception e) {
            sendError(PluginError.BIOMETRIC_UNKNOWN_ERROR);
        }
    }

    private void runBiometricActivity(PromptInfo promptInfo) {
        cordova.getActivity().runOnUiThread(() -> {
            Intent intent = new Intent(cordova.getActivity().getApplicationContext(), BiometricActivity.class);
            intent.putExtras(promptInfo.getBundle());
            this.cordova.startActivityForResult(this, intent, REQUEST_CODE_BIOMETRIC);
        });

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        this.mCallbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode != REQUEST_CODE_BIOMETRIC) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            sendError(intent);
            return;
        }
        sendSuccess(intent);
    }

    private void sendSuccess(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            sendSuccess(intent.getExtras().getString(PromptInfo.SECRET_EXTRA));
        } else {
            sendSuccess("biometric_success");
        }
    }

    private void sendSuccess(String message) {
        cordova.getActivity().runOnUiThread(() ->
            this.mCallbackContext.success(message));
    }

    private void sendSuccess(boolean result) {
        cordova.getActivity().runOnUiThread(() ->
            this.mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result)));
    }

    private void sendError(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            sendError(extras.getInt("code"), extras.getString("message"));
        } else {
            sendError(PluginError.BIOMETRIC_DISMISSED);
        }
    }

    private void sendError(int code, String message) {
        JSONObject resultJson = new JSONObject();
        try {
            resultJson.put("code", code);
            resultJson.put("message", message);

            PluginResult result = new PluginResult(PluginResult.Status.ERROR, resultJson);
            result.setKeepCallback(true);
            cordova.getActivity().runOnUiThread(() ->
                Fingerprint.this.mCallbackContext.sendPluginResult(result));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void sendError(PluginError error) {
        sendError(error.getValue(), error.getMessage());
    }

    private String getApplicationLabel(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo app = packageManager
                .getApplicationInfo(context.getPackageName(), 0);
            return packageManager.getApplicationLabel(app).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
