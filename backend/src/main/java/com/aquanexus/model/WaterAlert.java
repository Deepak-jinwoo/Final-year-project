package com.aquanexus.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity storing compliance, water balance, and usage alerts.
 */
@Entity
@Table(name = "water_alerts")
public class WaterAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "industry_id")
    private Integer industryId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "alert_date")
    private LocalDate date;

    @Column(name = "alert_type", length = 60)
    private String alertType;  // CTO_WARNING, CTO_CRITICAL, CTO_EXCEEDED, UNACCOUNTED_WATER, LOW_REUSE, UNTAPPED_REUSE, ABNORMAL_USAGE, DATA_QUALITY, MISSING_METER_READING, MONTHLY_COMPLIANCE_SUMMARY

    @Column(name = "severity", length = 20)
    private String severity;   // INFO, WARNING, CRITICAL

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "explanation", length = 1000)
    private String explanation;

    @Column(name = "status", length = 20)
    private String status = "OPEN"; // OPEN, ACKNOWLEDGED, RESOLVED

    @Column(name = "is_resolved")
    private Boolean isResolved = false;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "sms_sent")
    private Boolean smsSent = false;

    @Column(name = "last_notification_sent_at")
    private LocalDateTime lastNotificationSentAt;

    @Column(name = "notification_failure_reason", length = 500)
    private String notificationFailureReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "OPEN";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public WaterAlert() {}

    // Getters & Setters
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
    public void setStatus(String status) {
        this.status = status;
        this.isResolved = "RESOLVED".equalsIgnoreCase(status);
    }

    public Boolean getIsResolved() { return isResolved; }
    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
        if (Boolean.TRUE.equals(isResolved)) {
            this.status = "RESOLVED";
        }
    }

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
