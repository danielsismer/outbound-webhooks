package com.example.demo.domain.repository;

import com.example.demo.domain.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findBySubscriptionIdOrderByIdDesc(Long subscriptionId);

    List<WebhookDelivery> findByEventId(String eventId);
}
