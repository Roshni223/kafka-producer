package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OutboxEvent;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OutboxEventRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepo outboxEventRepo;
    private final ObjectMapper objectMapper;

    OrderService(OrderRepository orderRepository, OutboxEventRepo outboxEventRepo, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepo = outboxEventRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createOrder(Order order) {
        if (orderRepository.existsById(order.getOrderId())) {
            throw new IllegalStateException("Order already exists for id " + order.getOrderId());
        }

        order.setStatus("NEW");
        System.out.println("order :: " + order);
        orderRepository.save(order);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(order.getOrderId());
        event.setPayload(convertToJson(order));
        event.setStatus(OutboxEvent.Status.NEW.name());
        outboxEventRepo.save(event);
    }

    String convertToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            System.out.println();
            throw new RuntimeException("Error while converting object into json", e);
        }
    }

}
