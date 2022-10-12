package de.niklasmerz.cordova.biometric;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.UserNotAuthenticatedException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class BiometricActivity extends AppCompatActivity {
//    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 2;
    private PromptInfo mPromptInfo;
    private CryptographyManager mCryptographyManager;
    private BiometricPrompt mBiometricPrompt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(null);
        int layout = getResources()
            .getIdentifier("biometric_activity", "layout", getPackageName());
        setContentView(layout);

        if (savedInstanceState != null) {
            return;
        }

        mCryptographyManager = new CryptographyManagerImpl();
        mPromptInfo = new PromptInfo.Builder(getIntent().getExtras()).build();
        final Handler handler = new Handler(Looper.getMainLooper());
        Executor executor = handler::post;
        mBiometricPrompt = new BiometricPrompt(this, executor, mAuthenticationCallback);

        try {
            runAction();
        } catch (CryptoException e) {
            finishWithError(e);
        } catch (Exception e) {
            finishWithError(PluginError.BIOMETRIC_UNKNOWN_ERROR, e.getMessage());
        }
    }

    private void runAction() throws CryptoException {
        switch (mPromptInfo.getType()) {
            case CHALLENGE:
                challenge();
                return;
            case SET_SECRET:
                setSecret();
                return;
            case GET_SECRET:
                getSecret();
                return;
            case DELETE_SECRET:
                deleteSecret();
                return;
        }
        throw new CryptoException(PluginError.BIOMETRIC_ARGS_PARSING_FAILED);
    }

    private BiometricPrompt.PromptInfo createPromptInfo() {
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
            .setTitle(mPromptInfo.getTitle())
            .setSubtitle(mPromptInfo.getSubtitle())
            .setDescription(mPromptInfo.getDescription())
            .setConfirmationRequired(mPromptInfo.getConfirmationRequired());

        if (mPromptInfo.getLockBehavior() == LockBehavior.LOCK_AFTER_USE_BIOMETRIC_ONLY) {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG);
        } else {
            builder.setAllowedAuthenticators(
                BIOMETRIC_STRONG | DEVICE_CREDENTIAL
            );
        }

        // if (mPromptInfo.getLockBehavior() == LockBehavior.LOCK_AFTER_USE_PASSCODE_FALLBACK
        //         && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // TODO: remove after fix https://issuetracker.google.com/issues/142740104
        //     builder.setDeviceCredentialAllowed(true);
        // } else {
        //     builder.setNegativeButtonText(mPromptInfo.getCancelButtonTitle());
        // }

        return builder.build();
    }

    private void challenge() {
        mBiometricPrompt.authenticate(createPromptInfo());
    }

    private void setSecret() throws CryptoException {
        if (mPromptInfo.getSecret() == null) {
            throw new CryptoException(PluginError.BIOMETRIC_ARGS_PARSING_FAILED);
        }

        if (mPromptInfo.getBatch() != ActionBatchControl.START) {
            try {
                setSecretOnceAuthenticated();
                return;
            } catch (CryptoException e) {
                if (e.getCause() instanceof UserNotAuthenticatedException) {
                    if (mPromptInfo.getInteractionNotAllowed()) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        mBiometricPrompt.authenticate(createPromptInfo());
    }

    private void setSecretOnceAuthenticated() throws CryptoException {
        Cipher cipher = mCryptographyManager
            .getInitializedCipherForEncryption(mPromptInfo);
        String text = mPromptInfo.getSecret();
        EncryptedData encryptedData = mCryptographyManager
            .encryptData(text, cipher);
        encryptedData.save(mPromptInfo.getSecretName(), this);
        finishWithSuccess();
    }

    // private void authenticateToEncrypt() throws CryptoException {
    //     if (mPromptInfo.getSecret() == null) {
    //         throw new CryptoException(PluginError.BIOMETRIC_ARGS_PARSING_FAILED);
    //     }
    //     Cipher cipher = mCryptographyManager
    //         .getInitializedCipherForEncryption(mPromptInfo);
    //     mBiometricPrompt.authenticate(createPromptInfo(), new BiometricPrompt.CryptoObject(cipher));
    // }

    private void getSecret() throws CryptoException {
        if (mPromptInfo.getBatch() != ActionBatchControl.START) {
            try {
                getSecretOnceAuthenticated();
                return;
            } catch (CryptoException e) {
                if (e.getCause() instanceof UserNotAuthenticatedException) {
                    if (mPromptInfo.getInteractionNotAllowed()) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        mBiometricPrompt.authenticate(createPromptInfo());
    }

    private void getSecretOnceAuthenticated() throws CryptoException {
        byte[] initializationVector = EncryptedData
            .loadInitializationVector(mPromptInfo.getSecretName(), this);
        Cipher cipher = mCryptographyManager
            .getInitializedCipherForDecryption(mPromptInfo.getSecretName(), initializationVector);
        byte[] ciphertext = EncryptedData.loadCiphertext(mPromptInfo.getSecretName(), this);
        String secret = mCryptographyManager.decryptData(ciphertext, cipher);
        if (secret == null) {
            throw new CryptoException(PluginError.BIOMETRIC_NO_SECRET_FOUND);
        }
        Intent intent = new Intent();
        intent.putExtra(PromptInfo.SECRET_EXTRA, secret);
        finishWithSuccess(intent);
    }

    // private void authenticateToDecrypt() throws CryptoException {
    //     byte[] initializationVector = EncryptedData
    //         .loadInitializationVector(mPromptInfo.getSecretName(), this);
    //     Cipher cipher = mCryptographyManager
    //         .getInitializedCipherForDecryption(mPromptInfo.getSecretName(), initializationVector);
    //     mBiometricPrompt.authenticate(createPromptInfo(), new BiometricPrompt.CryptoObject(cipher));
    // }

    private void deleteSecret() throws CryptoException {
        if (mPromptInfo.getBatch() == ActionBatchControl.START) {
            try {
                deleteSecretOnceAuthenticated();
                return;
            } catch (CryptoException e) {
                if (e.getCause() instanceof UserNotAuthenticatedException) {
                    if (mPromptInfo.getInteractionNotAllowed()) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        mBiometricPrompt.authenticate(createPromptInfo());
    }

    private void deleteSecretOnceAuthenticated() throws CryptoException {
        mCryptographyManager.removeKey(mPromptInfo.getSecretName());
        finishWithSuccess();
    }

    private BiometricPrompt.AuthenticationCallback mAuthenticationCallback =
        new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                onError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {
                    switch (mPromptInfo.getType()) {
                        case SET_SECRET:
                            setSecretOnceAuthenticated();
                            break;
                        case GET_SECRET:
                            getSecretOnceAuthenticated();
                            break;
                        case DELETE_SECRET:
                            deleteSecretOnceAuthenticated();
                            break;
                        default:
                            finishWithSuccess();
                            break;
                    }
                } catch (CryptoException e) {
                    finishWithError(e);
                } catch (Exception e) {
                    finishWithError(PluginError.BIOMETRIC_UNKNOWN_ERROR, e.getMessage());
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                //onError(PluginError.BIOMETRIC_AUTHENTICATION_FAILED.getValue(), PluginError.BIOMETRIC_AUTHENTICATION_FAILED.getMessage());
            }
        };


    // TODO: remove after fix https://issuetracker.google.com/issues/142740104
    // private void showAuthenticationScreen() {
    //     KeyguardManager keyguardManager = ContextCompat
    //         .getSystemService(this, KeyguardManager.class);
    //     if (keyguardManager == null) {
    //         return;
    //     }
    //     if (keyguardManager.isKeyguardSecure()) {
    //         Intent intent = keyguardManager
    //             .createConfirmDeviceCredentialIntent(mPromptInfo.getTitle(), mPromptInfo.getDescription());
    //         this.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
    //     } else {
    //         // Show a message that the user hasn't set up a lock screen.
    //         finishWithError(PluginError.BIOMETRIC_SCREEN_GUARD_UNSECURED);
    //     }
    // }

    // TODO: remove after fix https://issuetracker.google.com/issues/142740104
    // @Override
    // public void onActivityResult(int requestCode, int resultCode, Intent data) {
    //     if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
    //         if (resultCode == Activity.RESULT_OK) {
    //             finishWithSuccess();
    //         } else {
    //             finishWithError(PluginError.BIOMETRIC_PIN_OR_PATTERN_DISMISSED);
    //         }
    //     }
    // }

    private void onError(int errorCode, @NonNull CharSequence errString) {
        switch (errorCode) {
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_CANCELED:
                finishWithError(PluginError.BIOMETRIC_DISMISSED);
                return;
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                // TODO: remove after fix https://issuetracker.google.com/issues/142740104
                // if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && mPromptInfo.isDeviceCredentialAllowed()) {
                //     showAuthenticationScreen();
                //     return;
                // }
                finishWithError(PluginError.BIOMETRIC_DISMISSED);
                break;
            case BiometricPrompt.ERROR_LOCKOUT:
                finishWithError(PluginError.BIOMETRIC_LOCKED_OUT.getValue(), errString.toString());
                break;
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                finishWithError(PluginError.BIOMETRIC_LOCKED_OUT_PERMANENT.getValue(), errString.toString());
                break;
            default:
                finishWithError(errorCode, errString.toString());
        }
    }

    private void finishWithSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    private void finishWithSuccess(Intent intent) {
        setResult(RESULT_OK, intent);
        finish();
    }

    // private void encrypt(BiometricPrompt.CryptoObject cryptoObject) throws CryptoException {
    //     String text = mPromptInfo.getSecret();
    //     EncryptedData encryptedData = mCryptographyManager.encryptData(text, cryptoObject.getCipher());
    //     encryptedData.save(mPromptInfo.getSecretName(), this);
    // }

    // private Intent getDecryptedIntent(BiometricPrompt.CryptoObject cryptoObject) throws CryptoException {
    //     byte[] ciphertext = EncryptedData.loadCiphertext(mPromptInfo.getSecretName(), this);
    //     String secret = mCryptographyManager.decryptData(ciphertext, cryptoObject.getCipher());
    //     if (secret != null) {
    //         Intent intent = new Intent();
    //         intent.putExtra(PromptInfo.SECRET_EXTRA, secret);
    //         return intent;
    //     }
    //     return null;
    // }

    private void finishWithError(CryptoException e) {
        finishWithError(e.getError().getValue(), e.getMessage());
    }

    private void finishWithError(PluginError error) {
        finishWithError(error.getValue(), error.getMessage());
    }

    private void finishWithError(PluginError error, String message) {
        finishWithError(error.getValue(), message);
    }

    private void finishWithError(int code, String message) {
        Intent data = new Intent();
        data.putExtra("code", code);
        data.putExtra("message", message);
        setResult(RESULT_CANCELED, data);
        finish();
    }
}
