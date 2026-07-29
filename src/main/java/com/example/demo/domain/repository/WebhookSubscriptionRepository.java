package com.example.demo.domain.repository;

import com.example.demo.domain.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByActiveIsTrueAndEventTypeIn(Collection<String> eventTypes);
}
