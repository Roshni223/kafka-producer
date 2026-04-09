package com.example.demo.conifg;

import com.example.demo.model.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;

@Configuration
public class KafkaConfig {
    @Bean
    public KafkaTransactionManager<String, Order> kafkaTransactionManager(
            ProducerFactory<String, Order> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }
}
