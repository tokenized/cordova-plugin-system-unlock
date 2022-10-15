package de.niklasmerz.cordova.biometric;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.UserNotAuthenticatedException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class BiometricActivity extends AppCompatActivity {
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

        if (mPromptInfo.getLockBehavior() == LockBehavior.LOCK_AFTER_USE_BIOMETRIC_ONLY
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG);
            builder.setNegativeButtonText(mPromptInfo.getCancelButtonTitle());
        } else {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL
            );
        }

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

    private void deleteSecret() throws CryptoException {
        if (mPromptInfo.getBatch() == ActionBatchControl.CONTINUE) {
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
                onError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
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
                onError(
                    PluginError.BIOMETRIC_AUTHENTICATION_FAILED.getValue(),
                    PluginError.BIOMETRIC_AUTHENTICATION_FAILED.getMessage()
                );
            }
        };

    private void onError(int errorCode, @NonNull CharSequence errString) {
        switch (errorCode) {
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_CANCELED:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                finishWithError(PluginError.BIOMETRIC_DISMISSED);
                return;
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
