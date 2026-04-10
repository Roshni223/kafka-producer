package com.example.demo.service;

import com.example.demo.avro.OrderCreated;
import com.example.demo.model.Order;
import com.example.demo.model.OutboxEvent;
import com.example.demo.repository.OutboxEventRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class OutboxPublisher {
    private final OutboxEventRepo outboxEventRepo;
    private final KafkaTemplate<String, OrderCreated> kafkaTemplate;
    private final ObjectMapper objectMapper;

    OutboxPublisher(KafkaTemplate<String, OrderCreated> kafkaTemplate,
                    OutboxEventRepo outboxEventRepo,
                    ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxEventRepo = outboxEventRepo;
        this.objectMapper = objectMapper;
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
            OrderCreated orderCreated = buildEvent(outboxEvent.getPayload());
            kafkaTemplate.send("order-topic", outboxEvent.getAggregateId(), orderCreated).get();
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

    private OrderCreated buildEvent(String payload) {
        try {
            Order order = objectMapper.readValue(payload, Order.class);
            return OrderCreated.newBuilder()
                    .setOrderId(order.getOrderId())
                    .setProduct(order.getProduct())
                    .setQuantity(order.getQuantity())
                    .setStatus(order.getStatus())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert outbox payload to Avro event", e);
        }
    }
}
