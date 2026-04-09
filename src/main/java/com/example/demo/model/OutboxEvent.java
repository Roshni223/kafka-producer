package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {
    public enum Status {
        NEW,
        PROCESSING,
        SENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String aggregateId;
    @Column(columnDefinition = "TEXT")
    String payload;
    String status;
}
