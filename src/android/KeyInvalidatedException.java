package com.tokenized.cordova.system_unlock;

class KeyInvalidatedException extends CryptoException {
    KeyInvalidatedException() {
        super(PluginError.BIOMETRIC_NO_SECRET_FOUND);
    }
}
