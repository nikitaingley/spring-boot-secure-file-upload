package com.oussema.cloud.multicloud;

public enum Cloud {
    AWS, AZURE;

    public Cloud other() {
        return this == AWS ? AZURE : AWS;
    }

    public static Cloud from(String value, String fallback) {
        String v = (value == null || value.isBlank()) ? fallback : value;
        return "aws".equalsIgnoreCase(v) ? AWS : AZURE;
    }
}