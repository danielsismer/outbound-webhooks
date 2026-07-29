package com.example.demo.application.listener;

import com.example.demo.config.AsyncConfig;
import com.example.demo.domain.event.UsuarioCriadoEvent;
import com.example.demo.infrastructure.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumidor do lado de escrita: reage a {@link UsuarioCriadoEvent} sem que o produtor
 * (UsuarioService) saiba da existencia de webhooks.
 *
 * <p>{@code AFTER_COMMIT} garante que o evento so sai depois de o dado estar realmente
 * gravado, e {@code @Async} tira a entrega do caminho da resposta HTTP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioEventListener {

    private final WebhookDispatcher dispatcher;

    @Async(AsyncConfig.WEBHOOK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUsuarioCriado(UsuarioCriadoEvent event) {
        log.info("Evento recebido: type={} eventId={}", event.eventType(), event.eventId());
        dispatcher.dispatch(event);
    }
}
