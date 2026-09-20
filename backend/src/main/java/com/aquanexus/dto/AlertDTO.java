package com.aquanexus.dto;

import com.aquanexus.model.WaterAlert;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AlertDTO {

    private Long id;
    private Integer industryId;
    private String department;
    private Long recordId;
    private LocalDate date;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private String explanation;
    private String status;
    private Boolean isResolved;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private Boolean notificationSent;
    private Boolean emailSent;
    private Boolean smsSent;
    private LocalDateTime lastNotificationSentAt;
    private String notificationFailureReason;
    private LocalDateTime createdAt;

    public AlertDTO() {}

    public static AlertDTO fromEntity(WaterAlert alert) {
        if (alert == null) return null;
        AlertDTO dto = new AlertDTO();
        dto.setId(alert.getId());
        dto.setIndustryId(alert.getIndustryId());
        dto.setDepartment(alert.getDepartment());
        dto.setRecordId(alert.getRecordId());
        dto.setDate(alert.getDate());
        dto.setAlertType(alert.getAlertType());
        dto.setSeverity(alert.getSeverity());
        dto.setTitle(alert.getTitle());
        dto.setMessage(alert.getMessage());
        dto.setExplanation(alert.getExplanation());
        dto.setStatus(alert.getStatus());
        dto.setIsResolved(alert.getIsResolved());
        dto.setResolvedBy(alert.getResolvedBy());
        dto.setResolvedAt(alert.getResolvedAt());
        dto.setResolutionNotes(alert.getResolutionNotes());
        dto.setNotificationSent(alert.getNotificationSent());
        dto.setEmailSent(alert.getEmailSent());
        dto.setSmsSent(alert.getSmsSent());
        dto.setLastNotificationSentAt(alert.getLastNotificationSentAt());
        dto.setNotificationFailureReason(alert.getNotificationFailureReason());
        dto.setCreatedAt(alert.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIndustryId() { return industryId; }
    public void setIndustryId(Integer industryId) { this.industryId = industryId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsResolved() { return isResolved; }
    public void setIsResolved(Boolean isResolved) { this.isResolved = isResolved; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }

    public Boolean getEmailSent() { return emailSent; }
    public void setEmailSent(Boolean emailSent) { this.emailSent = emailSent; }

    public Boolean getSmsSent() { return smsSent; }
    public void setSmsSent(Boolean smsSent) { this.smsSent = smsSent; }

    public LocalDateTime getLastNotificationSentAt() { return lastNotificationSentAt; }
    public void setLastNotificationSentAt(LocalDateTime lastNotificationSentAt) { this.lastNotificationSentAt = lastNotificationSentAt; }

    public String getNotificationFailureReason() { return notificationFailureReason; }
    public void setNotificationFailureReason(String notificationFailureReason) { this.notificationFailureReason = notificationFailureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
