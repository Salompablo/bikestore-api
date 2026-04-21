package com.bikestore.api.repository;

import com.bikestore.api.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
