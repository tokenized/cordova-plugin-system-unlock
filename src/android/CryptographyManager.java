package de.niklasmerz.cordova.biometric;

import javax.crypto.Cipher;

interface CryptographyManager {
    /**
     * Returns whether a secure key with the given name exists in the Android keystore
     */
    boolean hasKey(String keyName) throws CryptoException;

    /**
     * Creates a secure key in the Android keystore using the options specified,
     * and returns a Cipher ready to encrypt data with the key. Note that if
     * a key has previously been created it will be used without modification.
     */
    Cipher getInitializedCipherForEncryption(PromptInfo promptInfo) throws CryptoException;

    /**
     * Encrypts data using a Cipher set up by [getInitializedCipherForEncryption]
     */
    EncryptedData encryptData(String plaintext, Cipher cipher) throws CryptoException;

    /**
     * Finds a secure key in the Android keystore and returns a Cipher ready to
     * decrypt data with the key.
     */
    Cipher getInitializedCipherForDecryption(String keyName, byte[] initializationVector) throws CryptoException;

    /**
     * Decrypts data previously encrypted with [encryptData], using a Cipher set
     * up by [getInitializedCipherForDecryption].
     */
    String decryptData(byte[] ciphertext, Cipher cipher) throws CryptoException;

    /**
     * Removes a secure key with the given name from the Android keystore
     */
    void removeKey(String keyName) throws CryptoException;
}
