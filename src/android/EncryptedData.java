package com.tokenized.cordova.system_unlock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

class EncryptedData {
    private static final String DEFAULT_CIPHERTEXT_KEY_NAME = "__biometric-aio-ciphertext";
    private static final String DEFAULT_IV_KEY_NAME = "__biometric-aio-iv";

    private byte[] ciphertext;
    private byte[] initializationVector;

    EncryptedData(byte[] ciphertext, byte[] initializationVector) {
        this.ciphertext = ciphertext;
        this.initializationVector = initializationVector;
    }

    static byte[] loadInitializationVector(Context context) throws CryptoException {
        return load(DEFAULT_IV_KEY_NAME, context);
    }
    static byte[] loadInitializationVector(String keyName, Context context) throws CryptoException {
        if (keyName == null || PromptInfo.DEFAULT_SECRET_NAME.equals(keyName)) {
            return loadInitializationVector(context);
        }
        return load("SystemLock_iv_" + keyName, context);
    }

    static byte[] loadCiphertext(Context context) throws CryptoException {
        return load(DEFAULT_CIPHERTEXT_KEY_NAME, context);
    }
    static byte[] loadCiphertext(String keyName, Context context) throws CryptoException {
        if (keyName == null || PromptInfo.DEFAULT_SECRET_NAME.equals(keyName)) {
            return loadCiphertext(context);
        }
        return load("SystemLock_enc_" + keyName, context);
    }

    void save(Context context) {
        save(DEFAULT_IV_KEY_NAME, initializationVector, context);
        save(DEFAULT_CIPHERTEXT_KEY_NAME, ciphertext, context);
    }
    void save(String keyName, Context context) {
        if (keyName == null || PromptInfo.DEFAULT_SECRET_NAME.equals(keyName)) {
            save(context);
            return;
        }
        save("SystemLock_iv_" + keyName, initializationVector, context);
        save("SystemLock_enc_" + keyName, ciphertext, context);
    }

    private void save(String keyName, byte[] value, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences
            .edit()
            .putString(keyName, Base64.encodeToString(value, Base64.DEFAULT))
            .apply();
    }

    private static byte[] load(String keyName, Context context) throws CryptoException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String res = preferences.getString(keyName, null);
        if (res == null) {
            throw new CryptoException(PluginError.BIOMETRIC_NO_SECRET_FOUND);
        }
        return Base64.decode(res, Base64.DEFAULT);
    }
}
