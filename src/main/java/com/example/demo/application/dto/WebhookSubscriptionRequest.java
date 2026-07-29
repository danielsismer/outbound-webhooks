package com.example.demo.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebhookSubscriptionRequest(
        @NotBlank(message = "url e obrigatoria")
        @Pattern(regexp = "^https?://.+", message = "url deve comecar com http:// ou https://")
        @Size(max = 512, message = "url deve ter no maximo 512 caracteres")
        String url,

        @NotBlank(message = "eventType e obrigatorio")
        @Size(max = 100, message = "eventType deve ter no maximo 100 caracteres")
        String eventType,

        /** Opcional: quando ausente, a aplicacao gera um segredo e o devolve na resposta. */
        @Size(max = 128, message = "secret deve ter no maximo 128 caracteres")
        String secret
) {
}
