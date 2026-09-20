package com.aquanexus.controller;

import com.aquanexus.dto.AlertActionDTO;
import com.aquanexus.dto.AlertDTO;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.WaterAlertRepository;
import com.aquanexus.repository.WaterDailyRecordRepository;
import com.aquanexus.service.NotificationDispatcherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for alert management, acknowledging, resolving, and manual notification triggers.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final WaterAlertRepository alertRepository;
    private final WaterDailyRecordRepository recordRepository;
    private final NotificationDispatcherService dispatcherService;

    public AlertController(WaterAlertRepository alertRepository,
                           WaterDailyRecordRepository recordRepository,
                           NotificationDispatcherService dispatcherService) {
        this.alertRepository = alertRepository;
        this.recordRepository = recordRepository;
        this.dispatcherService = dispatcherService;
    }

    /**
     * GET /api/alerts
     * List alerts with optional filters: industryId, startDate, endDate, severity, type, status, department.
     */
    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAlerts(
            @RequestParam(required = false) Integer industryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department) {

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : null;
        String sev = (severity != null && !severity.isEmpty() && !"ALL".equalsIgnoreCase(severity)) ? severity : null;
        String aType = (type != null && !type.isEmpty() && !"ALL".equalsIgnoreCase(type)) ? type : null;
        String stat = (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) ? status : null;
        String dept = (department != null && !department.isEmpty() && !"ALL".equalsIgnoreCase(department)) ? department : null;

        List<WaterAlert> alerts = alertRepository.findFilteredAlerts(industryId, start, end, sev, aType, stat, dept);
        List<AlertDTO> dtoList = alerts.stream().map(AlertDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * GET /api/alerts/{id}
     * Get single alert details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAlertById(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(AlertDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/alerts/summary
     * Quick stats summary for dashboard cards and top header bell badge.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAlertsSummary() {
        Map<String, Object> summary = new HashMap<>();
        long totalOpen = alertRepository.countByIsResolvedFalse();
        long openCritical = alertRepository.countByIsResolvedFalseAndSeverity("CRITICAL");
        long openWarning = alertRepository.countByIsResolvedFalseAndSeverity("WARNING");
        long totalAll = alertRepository.count();

        summary.put("totalOpen", totalOpen);
        summary.put("openCritical", openCritical);
        summary.put("openWarning", openWarning);
        summary.put("totalAlerts", totalAll);
        summary.put("resolvedCount", totalAll - totalOpen);
        return ResponseEntity.ok(summary);
    }

    /**
     * PATCH /api/alerts/{id}/acknowledge
     * Acknowledge an alert.
     */
    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable Long id) {
        return alertRepository.findById(id).map(alert -> {
            alert.setStatus("ACKNOWLEDGED");
            alert.setUpdatedAt(LocalDateTime.now());
            WaterAlert saved = alertRepository.save(alert);
            return ResponseEntity.ok(AlertDTO.fromEntity(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/alerts/{id}/resolve
     * Resolve an alert with resolution notes.
     */
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id, @RequestBody(required = false) AlertActionDTO action) {
        return alertRepository.findById(id).map(alert -> {
            alert.setStatus("RESOLVED");
            alert.setIsResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            if (action != null) {
                if (action.getResolvedBy() != null) alert.setResolvedBy(action.getResolvedBy());
                if (action.getResolutionNotes() != null) alert.setResolutionNotes(action.getResolutionNotes());
            }
            if (alert.getResolvedBy() == null || alert.getResolvedBy().isEmpty()) {
                alert.setResolvedBy("Compliance Officer");
            }
            alert.setUpdatedAt(LocalDateTime.now());
            WaterAlert saved = alertRepository.save(alert);
            return ResponseEntity.ok(AlertDTO.fromEntity(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/alerts/{id}/resend-notification
     * Manually resend notification for an alert.
     */
    @PostMapping("/{id}/resend-notification")
    public ResponseEntity<?> resendNotification(@PathVariable Long id) {
        return alertRepository.findById(id).map(alert -> {
            WaterDailyRecord record = alert.getRecordId() != null
                    ? recordRepository.findById(alert.getRecordId()).orElse(null)
                    : null;

            dispatcherService.dispatchAlertNotification(alert, record);
            WaterAlert updated = alertRepository.findById(id).orElse(alert);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Notification dispatched successfully.");
            resp.put("alert", AlertDTO.fromEntity(updated));
            return ResponseEntity.ok(resp);
        }).orElse(ResponseEntity.notFound().build());
    }
}
