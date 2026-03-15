package com.fallguys.recruitment.service;

public record ReviewSyncPayload(
        String description,
        Object scores
) {
}
