package com.aquanexus.service;

import com.aquanexus.model.NotificationSetting;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.NotificationSettingRepository;
import com.aquanexus.repository.WaterAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Dispatcher service coordinating multichannel alert notifications (Email & SMS).
 * Ensures failures never block or crash parent transactions.
 */
@Service
public class NotificationDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherService.class);

    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final NotificationSettingRepository settingRepository;
    private final WaterAlertRepository alertRepository;

    public NotificationDispatcherService(EmailNotificationService emailService,
                                         SmsNotificationService smsService,
                                         NotificationSettingRepository settingRepository,
                                         WaterAlertRepository alertRepository) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.settingRepository = settingRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * Dispatches notifications for an alert based on industry notification preferences.
     */
    public void dispatchAlertNotification(WaterAlert alert, WaterDailyRecord record) {
        if (alert == null) return;

        try {
            Integer industryId = alert.getIndustryId() != null ? alert.getIndustryId() : 1;
            NotificationSetting setting = settingRepository.findByIndustryId(industryId)
                    .orElseGet(() -> {
                        NotificationSetting def = new NotificationSetting();
                        def.setIndustryId(industryId);
                        return settingRepository.save(def);
                    });

            String severity = alert.getSeverity() != null ? alert.getSeverity() : "INFO";
            String alertType = alert.getAlertType() != null ? alert.getAlertType() : "ALERT";

            // Check if this severity/type is enabled for notifications
            boolean shouldNotify = shouldNotifyForAlert(setting, severity, alertType);
            if (!shouldNotify) {
                log.info("Notification skipped for alert {} ({}) based on settings preference.", alert.getId(), alertType);
                return;
            }

            boolean emailOk = false;
            boolean smsOk = false;

            // 1. Email Channel
            if (Boolean.TRUE.equals(setting.getEmailNotificationsEnabled())) {
                String targetEmail = setting.getRegisteredEmail();
                if (targetEmail != null && !targetEmail.isEmpty()) {
                    emailOk = emailService.sendAlertEmail(alert, record, targetEmail);
                }
            }

            // 2. SMS Channel (By default for CRITICAL, EXCEEDED, and high-severity ABNORMAL_USAGE or UNACCOUNTED_WATER)
            if (Boolean.TRUE.equals(setting.getSmsNotificationsEnabled())) {
                boolean isSmsEligible = "CRITICAL".equalsIgnoreCase(severity) ||
                                       "CTO_EXCEEDED".equalsIgnoreCase(alertType) ||
                                       "CTO_CRITICAL".equalsIgnoreCase(alertType) ||
                                       ("ABNORMAL_USAGE".equalsIgnoreCase(alertType) && "CRITICAL".equalsIgnoreCase(severity)) ||
                                       ("UNACCOUNTED_WATER".equalsIgnoreCase(alertType) && "CRITICAL".equalsIgnoreCase(severity));

                if (isSmsEligible) {
                    String targetMobile = setting.getRegisteredMobile();
                    if (targetMobile != null && !targetMobile.isEmpty()) {
                        smsOk = smsService.sendAlertSms(alert, record, targetMobile);
                    }
                }
            }

            // Update alert flags
            alert.setNotificationSent(emailOk || smsOk);
            alert.setEmailSent(emailOk);
            alert.setSmsSent(smsOk);
            alert.setLastNotificationSentAt(LocalDateTime.now());
            if (!emailOk && !smsOk) {
                alert.setNotificationFailureReason("Notifications disabled or contact details missing in settings.");
            } else {
                alert.setNotificationFailureReason(null);
            }
            alertRepository.save(alert);

        } catch (Exception e) {
            log.error("Error during notification dispatch for alert {}: {}", alert.getId(), e.getMessage(), e);
            try {
                alert.setNotificationFailureReason("Dispatch error: " + e.getMessage());
                alertRepository.save(alert);
            } catch (Exception ignored) {}
        }
    }

    private boolean shouldNotifyForAlert(NotificationSetting setting, String severity, String alertType) {
        if ("CTO_EXCEEDED".equalsIgnoreCase(alertType)) {
            return Boolean.TRUE.equals(setting.getNotifyExceeded());
        }
        if ("ABNORMAL_USAGE".equalsIgnoreCase(alertType)) {
            return Boolean.TRUE.equals(setting.getNotifyAbnormalUsage());
        }
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return Boolean.TRUE.equals(setting.getNotifyCritical());
        }
        if ("WARNING".equalsIgnoreCase(severity)) {
            return Boolean.TRUE.equals(setting.getNotifyWarning());
        }
        return false;
    }
}
