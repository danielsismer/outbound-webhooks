package com.example.demo.infrastructure.webhook;

import com.example.demo.config.WebhookProperties;
import com.example.demo.domain.event.DomainEvent;
import com.example.demo.domain.model.DeliveryStatus;
import com.example.demo.domain.model.WebhookDelivery;
import com.example.demo.domain.model.WebhookSubscription;
import com.example.demo.domain.repository.WebhookDeliveryRepository;
import com.example.demo.domain.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Entrega um evento de dominio para todas as inscricoes ativas interessadas nele.
 *
 * <p>Cada tentativa e registrada em {@link WebhookDelivery}, o que da rastreabilidade
 * de quem recebeu o que — requisito basico de qualquer integracao por webhook.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final RestClient webhookRestClient;
    private final WebhookSigner signer;
    private final WebhookProperties properties;
    private final ObjectMapper objectMapper;

    public void dispatch(DomainEvent event) {
        List<WebhookSubscription> inscricoes = subscriptionRepository.findByActiveIsTrueAndEventTypeIn(
                List.of(event.eventType(), WebhookSubscription.ALL_EVENTS));

        if (inscricoes.isEmpty()) {
            log.debug("Nenhuma inscricao ativa para o evento {}", event.eventType());
            return;
        }

        String payload = serializar(event);
        inscricoes.forEach(inscricao -> entregar(inscricao, event, payload));
    }

    private String serializar(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(WebhookEnvelope.de(event));
        } catch (JacksonException e) {
            throw new IllegalStateException("Falha ao serializar evento " + event.eventType(), e);
        }
    }

    private void entregar(WebhookSubscription inscricao, DomainEvent event, String payload) {
        String assinatura = signer.sign(payload, inscricao.getSecret());

        Integer ultimoStatus = null;
        String ultimoErro = null;
        int tentativa = 0;

        while (tentativa < properties.maxAttempts()) {
            tentativa++;
            try {
                var resposta = webhookRestClient.post()
                        .uri(inscricao.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(properties.signatureHeader(), assinatura)
                        .header(properties.eventIdHeader(), event.eventId())
                        .header(properties.eventTypeHeader(), event.eventType())
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();

                log.info("Webhook entregue: subscription={} event={} status={} tentativas={}",
                        inscricao.getId(), event.eventId(), resposta.getStatusCode().value(), tentativa);

                registrar(inscricao, event, payload, DeliveryStatus.SUCCESS, tentativa,
                        resposta.getStatusCode().value(), null);
                return;

            } catch (RestClientResponseException e) {
                ultimoStatus = e.getStatusCode().value();
                ultimoErro = "HTTP " + ultimoStatus;
            } catch (RuntimeException e) {
                ultimoStatus = null;
                ultimoErro = descreverFalha(e);
            }

            log.warn("Falha na tentativa {}/{} para subscription={} event={}: {}",
                    tentativa, properties.maxAttempts(), inscricao.getId(), event.eventId(), ultimoErro);

            if (tentativa < properties.maxAttempts() && !esperarAntesDeRetentar(tentativa)) {
                break;
            }
        }

        registrar(inscricao, event, payload, DeliveryStatus.FAILED, tentativa, ultimoStatus, ultimoErro);
    }

    /**
     * Descreve a falha pela causa raiz: a mensagem do wrapper do Spring termina em
     * {@code ": null"} quando a excecao de rede nao tem mensagem propria, o que nao ajuda
     * quem for investigar a entrega depois.
     */
    private String descreverFalha(Throwable e) {
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String mensagem = raiz.getMessage();
        return raiz.getClass().getSimpleName() + (mensagem == null ? "" : ": " + mensagem);
    }

    /** Backoff linear. Retorna {@code false} se a thread foi interrompida (shutdown). */
    private boolean esperarAntesDeRetentar(int tentativa) {
        try {
            Thread.sleep(properties.retryDelay().toMillis() * tentativa);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void registrar(WebhookSubscription inscricao, DomainEvent event, String payload,
                           DeliveryStatus status, int tentativas, Integer responseStatus, String erro) {
        deliveryRepository.save(WebhookDelivery.builder()
                .subscriptionId(inscricao.getId())
                .eventId(event.eventId())
                .eventType(event.eventType())
                .url(inscricao.getUrl())
                .status(status)
                .attempts(tentativas)
                .responseStatus(responseStatus)
                .errorMessage(truncar(erro))
                .payload(payload)
                .build());
    }

    private String truncar(String erro) {
        if (erro == null || erro.length() <= 500) {
            return erro;
        }
        return erro.substring(0, 500);
    }
}
