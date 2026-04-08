package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class OrderService {
    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final OrderRepository orderRepository;
    OrderService(KafkaTemplate<String, Order> kafkaTemplate, OrderRepository orderRepository){
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }
    public void processOrder(Order order){
        order.setStatus("NEW");
        System.out.println("order :: " + order);
        orderRepository.save(order);
        kafkaTemplate.send("order-topic", order.getOrderId(), order);    }
}
