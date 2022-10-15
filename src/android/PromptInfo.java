package com.tokenized.cordova.system_unlock;

import android.os.Bundle;

import org.json.JSONArray;

class PromptInfo {
    private static final String BIOMETRIC_ACTIVITY_TYPE = "biometricActivityType";
    private static final String SECRET_NAME = "secretName";
    private static final String SECRET = "secret";
    private static final String SCOPE = "scope";
    private static final String LOCK_BEHAVIOR = "lockBehavior";
    private static final String ANDROID_AUTO_LOCK_TIME = "androidAutoLockTimeSeconds";
    private static final String NON_INTERACTIVE = "interactionNotAllowed";
    private static final String CONFIRMATION_REQUIRED = "confirmationRequired";
    private static final String BATCH = "batch";
    private static final String TITLE = "title";
    private static final String SUBTITLE = "subtitle";
    private static final String DESCRIPTION = "description";
    private static final String CANCEL_BUTTON_TITLE = "cancelButtonTitle";

    static final String DEFAULT_SECRET_NAME = "__aio_secret_key";

    static final String SECRET_EXTRA = "secret";

    private Bundle bundle = new Bundle();

    BiometricActivityType getType() {
        return BiometricActivityType.fromValue(bundle.getInt(BIOMETRIC_ACTIVITY_TYPE));
    }

    Bundle getBundle() {
        return bundle;
    }

    String getSecretName() {
        return bundle.getString(SECRET_NAME);
    }

    String getSecret() {
        return bundle.getString(SECRET);
    }

    SecretScope getScope() {
        return SecretScope.fromValue(bundle.getInt(SCOPE));
    }

    LockBehavior getLockBehavior() {
        return LockBehavior.fromValue(bundle.getInt(LOCK_BEHAVIOR));
    }

    int getAndroidAutoLockTimeSeconds() {
        return bundle.getInt(ANDROID_AUTO_LOCK_TIME);
    }

    boolean getInteractionNotAllowed() {
        return bundle.getBoolean(NON_INTERACTIVE);
    }

    boolean getConfirmationRequired() {
        return bundle.getBoolean(CONFIRMATION_REQUIRED);
    }

    ActionBatchControl getBatch() {
        return ActionBatchControl.fromValue(bundle.getInt(BATCH));
    }

    String getTitle() {
        return bundle.getString(TITLE);
    }

    String getSubtitle() {
        return bundle.getString(SUBTITLE);
    }

    String getDescription() {
        return bundle.getString(DESCRIPTION);
    }

    String getCancelButtonTitle() {
        return bundle.getString(CANCEL_BUTTON_TITLE);
    }

    public static final class Builder {
        private String defaultTitle = "App unlock";

        private Bundle bundle;
        private BiometricActivityType type = null;
        private String secretName = DEFAULT_SECRET_NAME;
        private String secret = null;
        private SecretScope scope = SecretScope.ONE_PASSCODE;
        private LockBehavior lockBehavior = LockBehavior.LOCK_AFTER_USE;
        private int androidAutoLockTimeSeconds = 14 * 24 * 60 * 60;
        private boolean interactionNotAllowed = false;
        private boolean confirmationRequired = true;
        private ActionBatchControl batch = null;
        private String title = "App unlock";
        private String subtitle = null;
        private String description = null;
        private String cancelButtonTitle = "Cancel";

        Builder(String applicationLabel) {
            if (applicationLabel != null) {
                defaultTitle = applicationLabel + " unlock";
            }
            title = defaultTitle;
        }

        Builder(Bundle bundle) {
            this.bundle = bundle;
        }

        public PromptInfo build() {
            PromptInfo promptInfo = new PromptInfo();

            if (this.bundle != null) {
                promptInfo.bundle = bundle;
                return promptInfo;
            }

            Bundle bundle = new Bundle();
            bundle.putInt(BIOMETRIC_ACTIVITY_TYPE, this.type.getValue());
            bundle.putString(SECRET_NAME, this.secretName);
            bundle.putString(SECRET, this.secret);
            bundle.putInt(SCOPE, this.scope.getValue());
            bundle.putInt(LOCK_BEHAVIOR, this.lockBehavior.getValue());
            bundle.putInt(ANDROID_AUTO_LOCK_TIME, this.androidAutoLockTimeSeconds);
            bundle.putBoolean(NON_INTERACTIVE, this.interactionNotAllowed);
            bundle.putBoolean(CONFIRMATION_REQUIRED, this.confirmationRequired);
            bundle.putInt(BATCH, this.batch.getValue());
            bundle.putString(SUBTITLE, this.subtitle);
            bundle.putString(TITLE, this.title);
            bundle.putString(DESCRIPTION, this.description);
            bundle.putString(CANCEL_BUTTON_TITLE, this.cancelButtonTitle);
            promptInfo.bundle = bundle;

            return promptInfo;
        }

        Builder parseArgs(JSONArray jsonArgs, BiometricActivityType type) {
            this.type = type;

            Args args = new Args(jsonArgs);
            secretName = args.getString(SECRET_NAME, DEFAULT_SECRET_NAME);
            secret = args.getString(SECRET, null);
            scope = SecretScope.fromJsonString(
                args.getString(SCOPE, "activeSystemLock")
            );
            lockBehavior = LockBehavior.fromJsonString(
                args.getString(LOCK_BEHAVIOR, "lockAfterUse")
            );
            androidAutoLockTimeSeconds = args.getInt(
                ANDROID_AUTO_LOCK_TIME, 14 * 24 * 60 * 60);
            interactionNotAllowed = args.getBoolean(NON_INTERACTIVE, false);
            confirmationRequired = args.getBoolean(CONFIRMATION_REQUIRED, true);
            batch = ActionBatchControl.fromJsonString(
                args.getString(BATCH, null)
            );
            title = args.getString(TITLE, defaultTitle);
            subtitle = args.getString(SUBTITLE, null);
            description = args.getString(DESCRIPTION, null);
            cancelButtonTitle = args.getString(CANCEL_BUTTON_TITLE, "Cancel");

            return this;
        }
    }
}
