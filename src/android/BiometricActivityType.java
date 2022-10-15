package com.tokenized.cordova.system_unlock;

import java.util.Objects;

public enum BiometricActivityType {
    IS_AVAILABLE(1, "isAvailable"),
    CHALLENGE(2, "challenge"),
    SET_SECRET(3, "setSecret"),
    HAS_SECRET(4, "hasSecret"),
    GET_SECRET(5, "getSecret"),
    DELETE_SECRET(6, "deleteSecret");

    private int value;
    private String jsonString;

    BiometricActivityType(int value, String jsonString) {
        this.value = value;
        this.jsonString = jsonString;
    }

    public int getValue() {
        return value;
    }

    public String getJsonString() {
        return jsonString;
    }

    public static BiometricActivityType fromValue(int val) {
        for (BiometricActivityType type : values()) {
            if (type.getValue() == val) {
                return type;
            }
        }
        return null;
    }

    public static BiometricActivityType fromJsonString(String jsonString) {
        for (BiometricActivityType scope : values()) {
            if (Objects.equals(scope.getJsonString(), jsonString)) {
                return scope;
            }
        }
        return null;
    }
}
