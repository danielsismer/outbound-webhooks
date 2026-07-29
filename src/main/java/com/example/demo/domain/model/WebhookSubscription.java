package com.example.demo.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Inscricao de um consumidor externo interessado em receber eventos via HTTP.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "webhook_subscription")
public class WebhookSubscription {

    /** Curinga aceito em {@link #eventType} para receber todos os eventos. */
    public static final String ALL_EVENTS = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL de destino que recebera o POST do webhook. */
    @Column(nullable = false, length = 512)
    private String url;

    /** Evento assinado, ou {@code *} para todos. */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** Segredo usado para assinar o payload com HMAC-SHA256. */
    @Column(nullable = false, length = 128)
    private String secret;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }
}
