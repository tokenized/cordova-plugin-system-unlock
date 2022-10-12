package de.niklasmerz.cordova.biometric;

public enum SecretScope {
    SYNC(1, "sync"),
    BACKUP(2, "backup"),
    ONE_DEVICE(3, "oneDevice"),
    ONE_PASSCODE(4, "onePasscode"),
    ONE_BIOMETRIC(5, "oneBiometric");

    private int value;
    private String jsonString;

    SecretScope(int value, String jsonString) {
        this.value = value;
        this.jsonString = jsonString;
    }

    public int getValue() {
        return value;
    }

    public String getJsonString() {
        return jsonString;
    }

    public static SecretScope fromValue(int val) {
        for (SecretScope scope : values()) {
            if (scope.getValue() == val) {
                return scope;
            }
        }
        return null;
    }

    public static SecretScope fromJsonString(String jsonString) {
        for (SecretScope scope : values()) {
            if (scope.getJsonString().equals(jsonString)) {
                return scope;
            }
        }
        return null;
    }
}
