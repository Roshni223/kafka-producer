package com.example.demo.repository;

import com.example.demo.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepo extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByIdAsc(String status);

    @Modifying
    @Query("update OutboxEvent e set e.status = :nextStatus where e.id = :id and e.status = :currentStatus")
    int updateStatusIfCurrent(Long id, String currentStatus, String nextStatus);
}
