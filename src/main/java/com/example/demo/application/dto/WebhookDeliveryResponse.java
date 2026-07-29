package com.example.demo.application.dto;

import com.example.demo.domain.model.DeliveryStatus;

import java.time.Instant;

public record WebhookDeliveryResponse(
        Long id,
        Long subscriptionId,
        String eventId,
        String eventType,
        String url,
        DeliveryStatus status,
        int attempts,
        Integer responseStatus,
        String errorMessage,
        Instant criadoEm
) {
}
