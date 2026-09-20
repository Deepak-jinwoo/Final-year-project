package com.aquanexus.dto;

import com.aquanexus.model.NotificationLog;
import java.time.LocalDateTime;

public class NotificationLogDTO {

    private Long id;
    private Long alertId;
    private Long recipientUserId;
    private String channel;
    private String recipientAddress;
    private String subject;
    private String message;
    private String deliveryStatus;
    private String providerResponse;
    private String errorMessage;
    private Integer attemptCount;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public NotificationLogDTO() {}

    public static NotificationLogDTO fromEntity(NotificationLog log) {
        if (log == null) return null;
        NotificationLogDTO dto = new NotificationLogDTO();
        dto.setId(log.getId());
        dto.setAlertId(log.getAlertId());
        dto.setRecipientUserId(log.getRecipientUserId());
        dto.setChannel(log.getChannel());
        dto.setRecipientAddress(log.getRecipientAddress());
        dto.setSubject(log.getSubject());
        dto.setMessage(log.getMessage());
        dto.setDeliveryStatus(log.getDeliveryStatus());
        dto.setProviderResponse(log.getProviderResponse());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setAttemptCount(log.getAttemptCount());
        dto.setSentAt(log.getSentAt());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }

    // Getters and Setters
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
