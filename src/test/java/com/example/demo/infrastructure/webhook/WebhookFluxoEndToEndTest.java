package com.example.demo.infrastructure.webhook;

import com.example.demo.application.dto.UsuarioRequest;
import com.example.demo.application.dto.WebhookSubscriptionRequest;
import com.example.demo.application.dto.WebhookSubscriptionResponse;
import com.example.demo.application.service.WebhookSubscriptionService;
import com.example.demo.domain.model.DeliveryStatus;
import com.example.demo.domain.model.WebhookDelivery;
import com.example.demo.domain.repository.WebhookDeliveryRepository;
import com.example.demo.domain.repository.WebhookSubscriptionRepository;
import com.example.demo.presentation.controller.WebhookReceiverController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercita o caminho completo: POST /api/usuarios -> commit -> evento -> webhook HTTP
 * entregue no receptor da propria aplicacao -> registro em webhook_delivery.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.webhook.max-attempts=2",
        "app.webhook.retry-delay=10ms"
})
class WebhookFluxoEndToEndTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Autowired
    private WebhookSubscriptionService subscriptionService;

    @Autowired
    private WebhookDeliveryRepository deliveryRepository;

    @Autowired
    private WebhookSubscriptionRepository subscriptionRepository;

    @Autowired
    private WebhookReceiverController receiver;

    @Autowired
    private WebhookSigner signer;

    @Autowired
    private ObjectMapper objectMapper;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
        receiver.limpar();
        deliveryRepository.deleteAll();
        // Sem isso, a inscricao de um teste continuaria recebendo os eventos do teste seguinte.
        subscriptionRepository.deleteAll();
    }

    @Test
    void deveEntregarWebhookAssinadoQuandoUsuarioECriado() {
        WebhookSubscriptionResponse inscricao = subscribe("/api/webhooks/receiver");

        criarUsuario("Ana");

        aguardar(() -> !receiver.listar().isEmpty(), "webhook nao foi recebido");

        WebhookReceiverController.WebhookRecebido recebido = receiver.listar().get(0);
        assertThat(recebido.eventType()).isEqualTo("usuario.criado");
        assertThat(recebido.eventId()).isNotBlank();

        // A assinatura precisa fechar com o segredo da inscricao.
        assertThat(signer.isValid(recebido.payload(), inscricao.secret(), recebido.signature())).isTrue();

        JsonNode payload = objectMapper.readTree(recebido.payload());
        assertThat(payload.get("eventType").asString()).isEqualTo("usuario.criado");
        assertThat(payload.get("eventId").asString()).isEqualTo(recebido.eventId());
        assertThat(payload.get("data").get("nome").asString()).isEqualTo("Ana");
        assertThat(payload.get("data").get("id").asLong()).isPositive();

        WebhookDelivery entrega = aguardarEntrega(inscricao.id());
        assertThat(entrega.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(entrega.getAttempts()).isEqualTo(1);
        assertThat(entrega.getResponseStatus()).isEqualTo(204);
        assertThat(entrega.getErrorMessage()).isNull();
    }

    @Test
    void deveRegistrarFalhaEEsgotarTentativasQuandoDestinoResponde404() {
        WebhookSubscriptionResponse inscricao = subscribe("/api/webhooks/destino-inexistente");

        criarUsuario("Bruno");

        WebhookDelivery entrega = aguardarEntrega(inscricao.id());
        assertThat(entrega.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(entrega.getAttempts()).isEqualTo(2);
        assertThat(entrega.getResponseStatus()).isEqualTo(404);
        assertThat(entrega.getErrorMessage()).contains("404");
        assertThat(receiver.listar()).isEmpty();
    }

    private WebhookSubscriptionResponse subscribe(String path) {
        WebhookSubscriptionResponse inscricao = subscriptionService.subscribe(new WebhookSubscriptionRequest(
                "http://localhost:" + port + path, "usuario.criado", null));
        assertThat(inscricao.secret()).isNotBlank();
        return inscricao;
    }

    private void criarUsuario(String nome) {
        var resposta = client.post()
                .uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UsuarioRequest(nome))
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
    }

    private WebhookDelivery aguardarEntrega(Long subscriptionId) {
        aguardar(() -> !deliveryRepository.findBySubscriptionIdOrderByIdDesc(subscriptionId).isEmpty(),
                "entrega da inscricao " + subscriptionId + " nao foi registrada");
        return deliveryRepository.findBySubscriptionIdOrderByIdDesc(subscriptionId).get(0);
    }

    /** O consumo e assincrono, entao o teste espera pela condicao em vez de assumir ordem. */
    private void aguardar(Supplier<Boolean> condicao, String mensagemDeFalha) {
        long limite = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < limite) {
            if (condicao.get()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("Timeout de " + TIMEOUT + ": " + mensagemDeFalha
                + " (entregas=" + descrever(deliveryRepository.findAll()) + ")");
    }

    private String descrever(List<WebhookDelivery> entregas) {
        return entregas.stream()
                .map(e -> e.getStatus() + "/" + e.getAttempts() + "/" + e.getErrorMessage())
                .toList()
                .toString();
    }
}
