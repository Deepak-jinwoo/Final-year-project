package com.aquanexus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit log entity tracking all notification dispatches (Email & SMS).
 */
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel; // EMAIL, SMS

    @Column(name = "recipient_address", length = 255, nullable = false)
    private String recipientAddress;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "delivery_status", length = 30, nullable = false)
    private String deliveryStatus; // PENDING, SENT, FAILED, SKIPPED, SIMULATED, NOT_CONFIGURED

    @Column(name = "provider_response", length = 1000)
    private String providerResponse;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempt_count")
    private Integer attemptCount = 1;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.attemptCount == null) this.attemptCount = 1;
    }

    public NotificationLog() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public Long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getProviderResponse() { return providerResponse; }
    public void setProviderResponse(String providerResponse) { this.providerResponse = providerResponse; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
