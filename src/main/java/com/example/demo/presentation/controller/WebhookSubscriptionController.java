package com.example.demo.presentation.controller;

import com.example.demo.application.dto.WebhookSubscriptionRequest;
import com.example.demo.application.dto.WebhookSubscriptionResponse;
import com.example.demo.application.service.WebhookSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/subscriptions")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionService service;

    @PostMapping
    public ResponseEntity<WebhookSubscriptionResponse> subscribe(
            @RequestBody @Valid WebhookSubscriptionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.subscribe(request));
    }

    @GetMapping
    public List<WebhookSubscriptionResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public WebhookSubscriptionResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/active")
    public WebhookSubscriptionResponse setActive(@PathVariable Long id, @RequestParam boolean value) {
        return service.setActive(id, value);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
