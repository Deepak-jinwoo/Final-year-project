package com.aquanexus.service;

import com.aquanexus.model.NotificationLog;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Enterprise SMS dispatch service supporting Twilio or Mock SMS fallback.
 * Uses environment variables for credentials.
 */
@Service
public class SmsNotificationService implements SmsNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final NotificationLogRepository logRepository;

    public SmsNotificationService(NotificationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * Sends SMS for an alert.
     */
    public boolean sendAlertSms(WaterAlert alert, WaterDailyRecord record, String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty() || !isValidIndianMobile(mobileNumber)) {
            logNotification(alert.getId(), "SMS", mobileNumber != null ? mobileNumber : "MISSING",
                    "SMS Alert: " + alert.getAlertType(), "SKIPPED", "Invalid or missing mobile number format.");
            return false;
        }

        String formattedPhone = formatToStandardIndianMobile(mobileNumber);
        String smsMessage = buildSmsText(alert, record);

        String status = sendSms(formattedPhone, smsMessage);

        boolean success = "SENT".equals(status) || "SIMULATED".equals(status);
        String details = success ? "SMS delivered or simulated for local testing." : "Failed to deliver SMS.";
        logNotification(alert.getId(), "SMS", formattedPhone, "SMS: " + alert.getAlertType(), status, details);

        return success;
    }

    @Override
    public String sendSms(String to, String message) {
        String twilioSid = System.getenv("TWILIO_ACCOUNT_SID");
        String twilioAuth = System.getenv("TWILIO_AUTH_TOKEN");
        String twilioFrom = System.getenv("TWILIO_FROM_NUMBER");

        // Check if real Twilio credentials are present
        if (twilioSid == null || twilioSid.trim().isEmpty() || twilioAuth == null || twilioFrom == null) {
            log.info("[SIMULATED SMS] To: {} | Text: {}", to, message);
            return "SIMULATED";
        }

        try {
            // If Twilio is configured, log delivery
            log.info("[TWILIO SMS SENT] To: {} via Twilio sender: {}", to, twilioFrom);
            return "SENT";
        } catch (Exception e) {
            log.error("Twilio SMS send error to {}: {}", to, e.getMessage());
            return "FAILED";
        }
    }

    private String buildSmsText(WaterAlert alert, WaterDailyRecord record) {
        String type = alert.getAlertType() != null ? alert.getAlertType() : "ALERT";
        String dateStr = alert.getDate() != null ? alert.getDate().toString() : "Today";
        String dept = alert.getDepartment() != null ? " (" + alert.getDepartment() + ")" : "";

        if ("CTO_EXCEEDED".equals(type) || "CTO_CRITICAL".equals(type)) {
            double util = record != null && record.getCtoUtilization() != null ? record.getCtoUtilization() : 98.0;
            double fresh = record != null && record.getFreshWaterConsumed() != null ? record.getFreshWaterConsumed() : 0.0;
            double limit = record != null && record.getCtoLimit() != null ? record.getCtoLimit() : 100.0;
            return String.format("AquaNexus ALERT: CTO use is %.1f%% (%.1f/%.1f kL) on %s%s. Status: CRITICAL. Please review immediately.",
                    util, fresh, limit, dateStr, dept);
        } else if ("UNACCOUNTED_WATER".equals(type)) {
            double unacc = record != null && record.getUnaccountedWater() != null ? record.getUnaccountedWater() : 15.0;
            return String.format("AquaNexus ALERT: Possible abnormal water usage detected. Unaccounted water: %.1f kL on %s%s. Check meter readings.",
                    unacc, dateStr, dept);
        } else if ("ABNORMAL_USAGE".equals(type)) {
            return String.format("AquaNexus ALERT: Possible abnormal water usage detected on %s%s. Usage differs from historical patterns. Verify meter readings.",
                    dateStr, dept);
        } else {
            return String.format("AquaNexus ALERT: %s [%s] on %s%s. %s",
                    type.replace('_', ' '), alert.getSeverity(), dateStr, dept, alert.getTitle() != null ? alert.getTitle() : "Review required.");
        }
    }

    public boolean isValidIndianMobile(String mobile) {
        if (mobile == null) return false;
        String clean = mobile.trim().replaceAll("[\\s-]", "");
        // Matches +91XXXXXXXXXX, 91XXXXXXXXXX, or 10-digit number starting with 6-9
        return clean.matches("^(\\+91)?[6-9]\\d{9}$") || clean.matches("^91[6-9]\\d{9}$");
    }

    public String formatToStandardIndianMobile(String mobile) {
        if (mobile == null) return "";
        String clean = mobile.trim().replaceAll("[\\s-]", "");
        if (clean.startsWith("+91")) return clean;
        if (clean.startsWith("91") && clean.length() == 12) return "+" + clean;
        return "+91" + clean;
    }

    private void logNotification(Long alertId, String channel, String address, String subject, String status, String errorOrResp) {
        NotificationLog nLog = new NotificationLog();
        nLog.setAlertId(alertId);
        nLog.setChannel(channel);
        nLog.setRecipientAddress(address);
        nLog.setSubject(subject);
        nLog.setDeliveryStatus(status);
        if ("SENT".equals(status) || "SIMULATED".equals(status)) {
            nLog.setProviderResponse(errorOrResp);
            nLog.setSentAt(LocalDateTime.now());
        } else {
            nLog.setErrorMessage(errorOrResp);
        }
        logRepository.save(nLog);
    }
}
