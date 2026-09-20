package com.aquanexus.controller;

import com.aquanexus.dto.NotificationLogDTO;
import com.aquanexus.model.NotificationLog;
import com.aquanexus.repository.NotificationLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for inspecting notification delivery logs.
 */
@RestController
@RequestMapping("/api/notification-logs")
public class NotificationLogController {

    private final NotificationLogRepository logRepository;

    public NotificationLogController(NotificationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * GET /api/notification-logs
     * Get recent notification logs (optionally filtered by alertId).
     */
    @GetMapping
    public ResponseEntity<List<NotificationLogDTO>> getLogs(@RequestParam(required = false) Long alertId) {
        List<NotificationLog> logs;
        if (alertId != null) {
            logs = logRepository.findByAlertIdOrderByCreatedAtDesc(alertId);
        } else {
            logs = logRepository.findTop50ByOrderByCreatedAtDesc();
        }
        List<NotificationLogDTO> dtoList = logs.stream().map(NotificationLogDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * GET /api/notification-logs/{id}
     * Get single notification log detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getLogById(@PathVariable Long id) {
        return logRepository.findById(id)
                .map(NotificationLogDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
