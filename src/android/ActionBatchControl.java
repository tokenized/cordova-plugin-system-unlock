package de.niklasmerz.cordova.biometric;

public enum ActionBatchControl {
    START(1, "start"),
    CONTINUE(2, "continue");

    private int value;
    private String jsonString;

    ActionBatchControl(int value, String jsonString) {
        this.value = value;
        this.jsonString = jsonString;
    }

    public int getValue() {
        return value;
    }

    public String getJsonString() {
        return jsonString;
    }

    public static ActionBatchControl fromValue(int val) {
        for (ActionBatchControl scope : values()) {
            if (scope.getValue() == val) {
                return scope;
            }
        }
        return null;
    }

    public static ActionBatchControl fromJsonString(String jsonString) {
        for (ActionBatchControl scope : values()) {
            if (scope.getJsonString().equals(jsonString)) {
                return scope;
            }
        }
        return null;
    }
}
