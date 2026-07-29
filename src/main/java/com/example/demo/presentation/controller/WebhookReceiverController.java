package com.example.demo.presentation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Consumidor de demonstracao: recebe os webhooks que a propria aplicacao envia,
 * permitindo testar o fluxo completo sem depender de um servico externo.
 *
 * <p>Guarda as ultimas {@value #CAPACIDADE} chamadas em memoria — e uma ferramenta de
 * demo/teste, nao um consumidor de producao.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/receiver")
public class WebhookReceiverController {

    private static final int CAPACIDADE = 50;

    public record WebhookRecebido(
            Instant recebidoEm,
            String eventId,
            String eventType,
            String signature,
            String payload
    ) {
    }

    private final Deque<WebhookRecebido> recebidos = new ArrayDeque<>();

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receber(
            @RequestHeader(value = "X-Webhook-Event-Id", required = false) String eventId,
            @RequestHeader(value = "X-Webhook-Event-Type", required = false) String eventType,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String payload) {

        log.info("Webhook recebido: type={} eventId={} signature={}", eventType, eventId, signature);

        synchronized (recebidos) {
            if (recebidos.size() == CAPACIDADE) {
                recebidos.removeFirst();
            }
            recebidos.addLast(new WebhookRecebido(Instant.now(), eventId, eventType, signature, payload));
        }
    }

    @GetMapping
    public List<WebhookRecebido> listar() {
        synchronized (recebidos) {
            return List.copyOf(recebidos);
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void limpar() {
        synchronized (recebidos) {
            recebidos.clear();
        }
    }
}
