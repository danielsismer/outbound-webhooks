package com.example.demo.application.dto;

import java.time.Instant;

public record WebhookSubscriptionResponse(
        Long id,
        String url,
        String eventType,
        String secret,
        boolean active,
        Instant criadoEm
) {
}
