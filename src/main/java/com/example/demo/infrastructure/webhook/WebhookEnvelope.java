package com.example.demo.infrastructure.webhook;

import com.example.demo.domain.event.DomainEvent;

import java.time.Instant;

/**
 * Formato do corpo enviado ao consumidor. Manter o envelope estavel permite evoluir
 * o conteudo de {@code data} sem quebrar quem ja consome os webhooks.
 */
public record WebhookEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        Object data
) {

    public static WebhookEnvelope de(DomainEvent event) {
        return new WebhookEnvelope(event.eventId(), event.eventType(), event.occurredAt(), event.data());
    }
}
