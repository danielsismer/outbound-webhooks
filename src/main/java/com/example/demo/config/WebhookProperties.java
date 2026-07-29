package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracao do envio de webhooks, prefixo {@code app.webhook} em application.properties.
 */
@ConfigurationProperties(prefix = "app.webhook")
public record WebhookProperties(
        /** Total de tentativas por entrega (1 = sem retry). */
        int maxAttempts,
        /** Espera base entre tentativas; cresce linearmente a cada tentativa. */
        Duration retryDelay,
        Duration connectTimeout,
        Duration readTimeout,
        String signatureHeader,
        String eventIdHeader,
        String eventTypeHeader
) {
}
