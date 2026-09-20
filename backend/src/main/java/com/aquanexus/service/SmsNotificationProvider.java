package com.aquanexus.service;

/**
 * Interface for SMS delivery providers.
 */
public interface SmsNotificationProvider {

    /**
     * Send an SMS message.
     * @param to Mobile phone number
     * @param message Text message content
     * @return Delivery status string (SENT, FAILED, SIMULATED, NOT_CONFIGURED)
     */
    String sendSms(String to, String message);
}
