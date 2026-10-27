package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.TradeEventOutbox;
import com.buzzleapyear.trading_api.repository.TradeEventOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class TradeEventOutboxPublisher {
    private static final Logger logger = LoggerFactory.getLogger(TradeEventOutboxPublisher.class);
    private final TradeEventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public TradeEventOutboxPublisher(
            TradeEventOutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.trade-order-topic}") String topic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        for (TradeEventOutbox event : outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafkaTemplate.send(topic, event.getAccountId().toString(), event.getPayload()).get(10, TimeUnit.SECONDS);
                event.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while publishing outbox event: {}", event.getEventId(), e);
            } catch (ExecutionException | TimeoutException e) {
                logger.error("Could not publish outbox event {}; it will be retried", event.getEventId(), e);
            }
        }
    }
}
