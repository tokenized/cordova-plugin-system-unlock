package de.niklasmerz.cordova.biometric;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

class CryptographyManagerImpl implements CryptographyManager {
    private static final String TAG = "CryptographyManagerImpl";

    private static final int KEY_SIZE = 256;
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String ENCRYPTION_PADDING = "NoPadding"; // KeyProperties.ENCRYPTION_PADDING_NONE
    private static final String ENCRYPTION_ALGORITHM = "AES"; // KeyProperties.KEY_ALGORITHM_AES
    private static final String KEY_ALGORITHM_AES = "AES"; // KeyProperties.KEY_ALGORITHM_AES
    private static final String ENCRYPTION_BLOCK_MODE = "GCM"; // KeyProperties.BLOCK_MODE_GCM

    private Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        String transformation = ENCRYPTION_ALGORITHM + "/" + ENCRYPTION_BLOCK_MODE + "/" + ENCRYPTION_PADDING;
        return Cipher.getInstance(transformation);
    }

    private SecretKey getOrCreateSecretKey(PromptInfo promptInfo) throws CryptoException {
        try {
            String keyName = promptInfo.getSecretName();

            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null); // Keystore must be loaded before it can be accessed

            SecretKey key = (SecretKey)keyStore.getKey(keyName, null);
            if (key != null) {
                return key;
            }

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                keyName,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(
                    promptInfo.getScope() == SecretScope.ONE_BIOMETRIC);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                if (promptInfo.getLockBehavior() == LockBehavior.LOCK_WITH_DEVICE) {
                    builder.setUserAuthenticationValidityDurationSeconds(
                        promptInfo.getAndroidAutoLockTimeSeconds());
                } else {
                    builder.setUserAuthenticationValidityDurationSeconds(60 * 5);
                }
            } else {
                switch (promptInfo.getLockBehavior()) {
                    case LOCK_WITH_DEVICE:
                        builder.setUserAuthenticationParameters(
                            promptInfo.getAndroidAutoLockTimeSeconds(),
                            KeyProperties.AUTH_BIOMETRIC_STRONG
                                | KeyProperties.AUTH_DEVICE_CREDENTIAL
                        );
                        break;
                    case LOCK_AFTER_USE:
                        builder.setUserAuthenticationParameters(
                            60 * 5,
                            KeyProperties.AUTH_BIOMETRIC_STRONG
                                | KeyProperties.AUTH_DEVICE_CREDENTIAL
                        );
                        break;
                    case LOCK_AFTER_USE_BIOMETRIC_ONLY:
                        builder.setUserAuthenticationParameters(
                            60 * 5,
                            KeyProperties.AUTH_BIOMETRIC_STRONG
                        );
                        break;
                }
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            keyGenerator.init(builder.build());

            return keyGenerator.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "getOrCreateSecretKey " + promptInfo.getSecretName() + " error", e);
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public Cipher getInitializedCipherForEncryption(PromptInfo promptInfo) throws CryptoException {
        try {
            try {
                return tryInitializeCipherForEncryption(promptInfo);
            } catch (KeyInvalidatedException e) {
                // If existing key is invalidated, delete and try again
                Log.d(TAG, "getInitializedCipherForEncryption: deleting invalidated key " + promptInfo.getSecretName());
                removeKey(promptInfo.getSecretName());
                return tryInitializeCipherForEncryption(promptInfo);
            }
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    private Cipher tryInitializeCipherForEncryption(PromptInfo promptInfo) throws CryptoException {
        try {
            Cipher cipher = getCipher();
            SecretKey secretKey = getOrCreateSecretKey(promptInfo);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher;
        } catch (CryptoException e) {
            if (e.getCause() instanceof KeyPermanentlyInvalidatedException
                    || e.getCause() instanceof UnrecoverableKeyException) {
                throw new KeyInvalidatedException();
            }
            throw e;
        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public EncryptedData encryptData(String plaintext, Cipher cipher) throws CryptoException {
        try {
            byte[] ciphertext = cipher.doFinal(
                plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedData(ciphertext, cipher.getIV());
        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public Cipher getInitializedCipherForDecryption(String keyName, byte[] initializationVector) throws CryptoException {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)keyStore.getKey(keyName, null);
            if (secretKey == null) {
                throw new CryptoException(PluginError.BIOMETRIC_NO_SECRET_FOUND);
            }

            Cipher cipher = getCipher();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, initializationVector));
            return cipher;
        } catch (CryptoException e) {
            throw e;
        } catch (UnrecoverableKeyException | KeyPermanentlyInvalidatedException e) {
            Log.d(TAG, "getInitializedCipherForDecryption: invalidated key " + keyName);
            throw new KeyInvalidatedException();
        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public String decryptData(byte[] ciphertext, Cipher cipher) throws CryptoException {
        try {
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public void removeKey(String keyName) throws CryptoException {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            keyStore.deleteEntry(keyName);
        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }
}
