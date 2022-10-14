package de.niklasmerz.cordova.biometric;

import java.util.Objects;

public enum LockBehavior {
    LOCK_WITH_DEVICE(1, "lockWithDevice"),
    LOCK_AFTER_USE(2, "lockAfterUse"),
    LOCK_AFTER_USE_BIOMETRIC_ONLY(3, "lockAfterUseBiometricOnly");

    private int value;
    private String jsonString;

    LockBehavior(int value, String jsonString) {
        this.value = value;
        this.jsonString = jsonString;
    }

    public int getValue() {
        return value;
    }

    public String getJsonString() {
        return jsonString;
    }

    public static LockBehavior fromValue(int val) {
        for (LockBehavior scope : values()) {
            if (scope.getValue() == val) {
                return scope;
            }
        }
        return null;
    }

    public static LockBehavior fromJsonString(String jsonString) {
        for (LockBehavior scope : values()) {
            if (Objects.equals(scope.getJsonString(), jsonString)) {
                return scope;
            }
        }
        return null;
    }
}
