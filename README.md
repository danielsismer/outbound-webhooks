# Demo — EDA + Webhooks

API Spring Boot que publica eventos de dominio e os entrega a consumidores externos via webhook HTTP.

## Fluxo

```
POST /api/usuarios
      │
      ▼
UsuarioService  ──(@Transactional: salva no banco)
      │
      └─ publishEvent(UsuarioCriadoEvent)
                  │
                  ▼  AFTER_COMMIT + @Async (pool "webhook-")
         UsuarioEventListener
                  │
                  ▼
         WebhookDispatcher
                  ├─ busca inscricoes ativas para "usuario.criado" (ou "*")
                  ├─ serializa o envelope, assina com HMAC-SHA256
                  ├─ POST para a url do consumidor (com retry)
                  └─ grava o resultado em webhook_delivery
```

Dois pontos garantem o comportamento correto:

- **`AFTER_COMMIT`**: se a transacao der rollback, nenhum webhook e enviado — nunca se anuncia um
  usuario que nao existe no banco.
- **`@Async`**: a entrega sai do caminho da resposta HTTP, então um consumidor lento ou fora do ar
  nao degrada o tempo de resposta da API.

O produtor (`UsuarioService`) nao conhece webhooks; ele só publica o evento. Adicionar outro
consumidor (e-mail, métrica, fila) é criar um novo listener, sem tocar no service.

## Camadas

| Pacote | Responsabilidade |
| --- | --- |
| `domain.model` | Entidades JPA: `Usuario`, `WebhookSubscription`, `WebhookDelivery` |
| `domain.event` | `DomainEvent` (contrato) e `UsuarioCriadoEvent` |
| `domain.repository` | Repositórios Spring Data |
| `application.service` | Casos de uso e publicação de eventos |
| `application.listener` | Consumidores dos eventos |
| `infrastructure.webhook` | `WebhookDispatcher`, `WebhookSigner`, `WebhookEnvelope` |
| `presentation.controller` | Controllers REST + receptor de demonstração |

## Rodando

```bash
./mvnw spring-boot:run
```

Se a 8080 estiver ocupada (por exemplo pelo ScadaBR), suba em outra porta:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Console do H2: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:demo`, usuário `sa`, sem senha).

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/usuarios` | Cria usuário e dispara o evento |
| `POST` | `/api/webhooks/subscriptions` | Inscreve um consumidor (devolve o `secret` **só nesta resposta**) |
| `GET` | `/api/webhooks/subscriptions` | Lista inscrições |
| `GET` | `/api/webhooks/subscriptions/{id}` | Detalha uma inscrição |
| `PATCH` | `/api/webhooks/subscriptions/{id}/active?value=false` | Ativa/desativa |
| `DELETE` | `/api/webhooks/subscriptions/{id}` | Remove |
| `GET` | `/api/webhooks/subscriptions/{id}/deliveries` | Histórico de entregas (status HTTP, tentativas, erro) |
| `POST` | `/api/webhooks/receiver` | Receptor de demonstração (a aplicação recebe o próprio webhook) |
| `GET` | `/api/webhooks/receiver` | Últimos webhooks recebidos pelo receptor |
| `DELETE` | `/api/webhooks/receiver` | Limpa o receptor |

## Teste manual

> Os exemplos usam a porta **8080**. Se você subiu em outra (`--server.port=8081`), troque a porta
> em todas as URLs — inclusive na `url` da inscrição, que é para onde a própria aplicação vai
> postar o webhook.

Inscrever o receptor local (use `*` em `eventType` para receber todos os eventos):

```bash
curl -X POST http://localhost:8080/api/webhooks/subscriptions -H "Content-Type: application/json" -d "{\"url\":\"http://localhost:8080/api/webhooks/receiver\",\"eventType\":\"usuario.criado\"}"
```

Criar um usuário:

```bash
curl -X POST http://localhost:8080/api/usuarios -H "Content-Type: application/json" -d "{\"nome\":\"Ana\"}"
```

Ver o webhook que chegou:

```bash
curl http://localhost:8080/api/webhooks/receiver
```

Ver o histórico de entregas da inscrição 1:

```bash
curl http://localhost:8080/api/webhooks/subscriptions/1/deliveries
```

## Payload enviado ao consumidor

```json
{
  "eventId": "b1e7...-...",
  "eventType": "usuario.criado",
  "occurredAt": "2026-07-29T12:00:00.123Z",
  "data": { "id": 1, "nome": "Ana", "criadoEm": "2026-07-29T12:00:00.100Z" }
}
```

Headers acompanhando o POST:

| Header | Conteúdo |
| --- | --- |
| `X-Webhook-Event-Id` | UUID do evento — use para **idempotência**, já que um retry pode reentregar o mesmo evento |
| `X-Webhook-Event-Type` | `usuario.criado` |
| `X-Webhook-Signature` | `sha256=<hex>` — HMAC-SHA256 do corpo bruto usando o `secret` da inscrição |

O consumidor valida a assinatura recalculando o HMAC sobre o corpo **bruto** recebido e comparando
em tempo constante (ver `WebhookSigner.isValid`).

## Configuração (`application.properties`)

| Propriedade | Padrão | Efeito |
| --- | --- | --- |
| `app.webhook.max-attempts` | `3` | Tentativas por entrega (`1` desliga o retry) |
| `app.webhook.retry-delay` | `500ms` | Espera base entre tentativas, com backoff linear |
| `app.webhook.connect-timeout` | `3s` | Timeout de conexão |
| `app.webhook.read-timeout` | `5s` | Timeout de leitura |

## Testes

```bash
./mvnw test
```

- `UsuarioServiceTest` — confirma que o service publica `UsuarioCriadoEvent`.
- `WebhookFluxoEndToEndTest` — sobe a aplicação em porta aleatória, cria um usuário por HTTP e
  verifica que o webhook chega assinado no receptor, com a entrega registrada como `SUCCESS`;
  o segundo caso aponta a inscrição para uma URL que responde 404 e verifica que as tentativas
  se esgotam e a entrega fica `FAILED`.

## Limitações conhecidas

- O retry acontece só dentro da chamada assíncrona. Se a aplicação cair no meio da entrega, o
  evento é perdido — a solução usual é o padrão **outbox**: gravar o evento na mesma transação do
  dado e ter um job que varre e reenvia os pendentes.
- Os eventos são in-process (`ApplicationEventPublisher`). Não sobrevivem a restart nem cruzam
  instâncias; para isso, o publish passaria a ir para um broker (RabbitMQ/Kafka) no lugar do
  listener local.
- O `secret` fica em texto no banco, o que basta para o exercício mas não para produção.
- O H2 é **em memória**: cada restart zera usuários, inscrições e histórico de entregas. Depois de
  reiniciar é preciso inscrever o consumidor novamente.
