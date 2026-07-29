package com.example.demo.application.service;

import com.example.demo.application.dto.WebhookSubscriptionRequest;
import com.example.demo.application.dto.WebhookSubscriptionResponse;
import com.example.demo.application.mapper.WebhookMapper;
import com.example.demo.domain.exception.NotFoundException;
import com.example.demo.domain.model.WebhookSubscription;
import com.example.demo.domain.repository.WebhookSubscriptionRepository;
import com.example.demo.infrastructure.webhook.WebhookSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository repository;
    private final WebhookMapper mapper;
    private final WebhookSigner signer;

    @Transactional
    public WebhookSubscriptionResponse subscribe(WebhookSubscriptionRequest request) {
        String secret = StringUtils.hasText(request.secret())
                ? request.secret()
                : signer.generateSecret();

        WebhookSubscription subscription = repository.save(WebhookSubscription.builder()
                .url(request.url())
                .eventType(request.eventType())
                .secret(secret)
                .active(true)
                .build());

        return mapper.toResponse(subscription, true);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscriptionResponse> findAll() {
        return repository.findAll().stream()
                .map(subscription -> mapper.toResponse(subscription, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public WebhookSubscriptionResponse findById(Long id) {
        return mapper.toResponse(buscar(id), false);
    }

    @Transactional
    public WebhookSubscriptionResponse setActive(Long id, boolean active) {
        WebhookSubscription subscription = buscar(id);
        subscription.setActive(active);
        return mapper.toResponse(subscription, false);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(buscar(id));
    }

    private WebhookSubscription buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inscricao de webhook %d nao encontrada".formatted(id)));
    }
}
