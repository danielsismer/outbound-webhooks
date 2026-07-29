package com.example.demo.domain.event;

import java.time.Instant;

/**
 * Contrato comum a todo evento de dominio publicado pela aplicacao.
 * O dispatcher de webhooks so conhece esta abstracao, nunca os eventos concretos.
 */
public interface DomainEvent {

    /** Identificador unico do evento, usado para idempotencia no consumidor. */
    String eventId();

    /** Nome do evento no formato {@code recurso.acao}, ex.: {@code usuario.criado}. */
    String eventType();

    /** Momento em que o fato aconteceu. */
    Instant occurredAt();

    /** Corpo de negocio do evento, serializado como {@code data} no payload do webhook. */
    Object data();
}
