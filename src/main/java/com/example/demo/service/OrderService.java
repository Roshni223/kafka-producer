package com.example.demo.service;

import com.example.demo.model.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class OrderService {
    private final KafkaTemplate<String, Order> kafkaTemplate;
    OrderService(KafkaTemplate<String, Order> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }
    public void processOrder(Order order){
        System.out.println("sending order " + order.orderId() + " kafka");
        kafkaTemplate.send("order-topic", order.orderId(), order);    }
}
