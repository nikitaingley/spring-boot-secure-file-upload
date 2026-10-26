package com.oussema.cloud.model;

public record Blob(
        String name,
        String type,
        String key,
        Long size) {
}
