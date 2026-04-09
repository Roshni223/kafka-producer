package com.example.demo.service;

import com.example.demo.model.OutboxEvent;
import com.example.demo.repository.OutboxEventRepo;
import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class OutboxPublisher {
    private final OutboxEventRepo outboxEventRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    OutboxPublisher(KafkaTemplate<String, String> kafkaTemplate, OutboxEventRepo outboxEventRepo) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxEventRepo = outboxEventRepo;
    }

    @Scheduled(fixedDelay = 5000)
    public void publish() {
        List<OutboxEvent> eventList = outboxEventRepo.findTop50ByStatusOrderByIdAsc(OutboxEvent.Status.NEW.name());
        for (OutboxEvent outboxEvent : eventList) {
            publishSingleEvent(outboxEvent);
        }
    }

    @Transactional
    void publishSingleEvent(OutboxEvent outboxEvent) {
        int claimed = outboxEventRepo.updateStatusIfCurrent(
                outboxEvent.getId(),
                OutboxEvent.Status.NEW.name(),
                OutboxEvent.Status.PROCESSING.name()
        );
        if (claimed == 0) {
            return;
        }

        try {
            kafkaTemplate.send("order-topic", outboxEvent.getAggregateId(), outboxEvent.getPayload()).get();
            outboxEventRepo.updateStatusIfCurrent(
                    outboxEvent.getId(),
                    OutboxEvent.Status.PROCESSING.name(),
                    OutboxEvent.Status.SENT.name()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resetForRetry(outboxEvent.getId());
            throw new RuntimeException("Interrupted while publishing outbox event " + outboxEvent.getId(), e);
        } catch (ExecutionException e) {
            resetForRetry(outboxEvent.getId());
            throw new RuntimeException("Kafka publish failed for outbox event " + outboxEvent.getId(), e);
        }
    }

    private void resetForRetry(Long id) {
        outboxEventRepo.updateStatusIfCurrent(
                id,
                OutboxEvent.Status.PROCESSING.name(),
                OutboxEvent.Status.NEW.name()
        );
    }
}
