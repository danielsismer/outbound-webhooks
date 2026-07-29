package com.example.demo.application.mapper;

import com.example.demo.application.dto.WebhookSubscriptionResponse;
import com.example.demo.domain.model.WebhookSubscription;
import org.springframework.stereotype.Component;

@Component
public class WebhookMapper {

    /**
     * O segredo so e exposto na criacao da inscricao; nas consultas ele fica oculto.
     */
    public WebhookSubscriptionResponse toResponse(WebhookSubscription subscription, boolean revelarSecret) {
        return new WebhookSubscriptionResponse(
                subscription.getId(),
                subscription.getUrl(),
                subscription.getEventType(),
                revelarSecret ? subscription.getSecret() : null,
                subscription.isActive(),
                subscription.getCriadoEm()
        );
    }
}
