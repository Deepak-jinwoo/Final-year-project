package com.aquanexus.repository;

import com.aquanexus.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByAlertIdOrderByCreatedAtDesc(Long alertId);

    List<NotificationLog> findAllByOrderByCreatedAtDesc();

    List<NotificationLog> findTop50ByOrderByCreatedAtDesc();
}
