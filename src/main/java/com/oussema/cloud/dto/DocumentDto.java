package com.oussema.cloud.dto;

import java.util.UUID;

public record DocumentDto(
        UUID id,
        String name,
        String type,
        String url,
        Long size,
        String provider,
        String servedFrom,
        String replicaStatus) {
}