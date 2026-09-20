package com.aquanexus.controller;

import com.aquanexus.dto.NotificationSettingDTO;
import com.aquanexus.model.NotificationSetting;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.repository.NotificationSettingRepository;
import com.aquanexus.service.EmailNotificationService;
import com.aquanexus.service.SmsNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing notification settings and testing dispatch.
 */
@RestController
@RequestMapping("/api/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingRepository settingRepository;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;

    public NotificationSettingsController(NotificationSettingRepository settingRepository,
                                          EmailNotificationService emailService,
                                          SmsNotificationService smsService) {
        this.settingRepository = settingRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    /**
     * GET /api/notification-settings
     * Retrieve notification preferences.
     */
    @GetMapping
    public ResponseEntity<NotificationSettingDTO> getSettings(@RequestParam(required = false, defaultValue = "1") Integer industryId) {
        NotificationSetting setting = settingRepository.findByIndustryId(industryId)
                .orElseGet(() -> {
                    NotificationSetting def = new NotificationSetting();
                    def.setIndustryId(industryId);
                    return settingRepository.save(def);
                });
        return ResponseEntity.ok(NotificationSettingDTO.fromEntity(setting));
    }

    /**
     * PUT /api/notification-settings
     * Update notification preferences.
     */
    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody NotificationSettingDTO dto) {
        Integer indId = dto.getIndustryId() != null ? dto.getIndustryId() : 1;
        NotificationSetting setting = settingRepository.findByIndustryId(indId)
                .orElseGet(() -> {
                    NotificationSetting s = new NotificationSetting();
                    s.setIndustryId(indId);
                    return s;
                });

        // Validation for email & Indian mobile
        if (dto.getRegisteredEmail() != null && !dto.getRegisteredEmail().isEmpty()) {
            if (!emailService.isValidEmail(dto.getRegisteredEmail())) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Invalid email address format.");
                return ResponseEntity.badRequest().body(err);
            }
        }

        if (dto.getRegisteredMobile() != null && !dto.getRegisteredMobile().isEmpty()) {
            if (!smsService.isValidIndianMobile(dto.getRegisteredMobile())) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Invalid Indian mobile number. Expected +91XXXXXXXXXX or 10 digits starting with 6-9.");
                return ResponseEntity.badRequest().body(err);
            }
            dto.setRegisteredMobile(smsService.formatToStandardIndianMobile(dto.getRegisteredMobile()));
        }

        dto.updateEntity(setting);
        NotificationSetting saved = settingRepository.save(setting);
        return ResponseEntity.ok(NotificationSettingDTO.fromEntity(saved));
    }

    /**
     * POST /api/notification-settings/test-email
     * Send test email notification.
     */
    @PostMapping("/test-email")
    public ResponseEntity<?> sendTestEmail(@RequestParam(required = false) String email) {
        NotificationSetting setting = settingRepository.findByIndustryId(1).orElse(new NotificationSetting());
        String target = (email != null && !email.isEmpty()) ? email : setting.getRegisteredEmail();

        if (target == null || !emailService.isValidEmail(target)) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Please provide a valid registered email address.");
            return ResponseEntity.badRequest().body(err);
        }

        WaterAlert testAlert = new WaterAlert();
        testAlert.setId(0L);
        testAlert.setDate(LocalDate.now());
        testAlert.setDepartment("General Production");
        testAlert.setAlertType("CTO_WARNING");
        testAlert.setSeverity("WARNING");
        testAlert.setTitle("AquaNexus Test Notification");
        testAlert.setMessage("This is a verified test email sent from your AquaNexus Notification Module.");
        testAlert.setExplanation("All email dispatch channels and notification logging services are operational.");

        boolean ok = emailService.sendAlertEmail(testAlert, null, target);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", ok);
        resp.put("target", target);
        resp.put("message", ok ? "Test email dispatched successfully (Check server logs if in simulated mode)." : "Failed to dispatch email.");
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/notification-settings/test-sms
     * Send test SMS notification.
     */
    @PostMapping("/test-sms")
    public ResponseEntity<?> sendTestSms(@RequestParam(required = false) String mobile) {
        NotificationSetting setting = settingRepository.findByIndustryId(1).orElse(new NotificationSetting());
        String target = (mobile != null && !mobile.isEmpty()) ? mobile : setting.getRegisteredMobile();

        if (target == null || !smsService.isValidIndianMobile(target)) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Please provide a valid Indian mobile number (+91XXXXXXXXXX or 10 digits).");
            return ResponseEntity.badRequest().body(err);
        }

        WaterAlert testAlert = new WaterAlert();
        testAlert.setId(0L);
        testAlert.setDate(LocalDate.now());
        testAlert.setDepartment("Plant 1");
        testAlert.setAlertType("CTO_CRITICAL");
        testAlert.setSeverity("CRITICAL");
        testAlert.setTitle("AquaNexus Test SMS");
        testAlert.setMessage("AquaNexus TEST ALERT: Notification system is operational.");

        boolean ok = smsService.sendAlertSms(testAlert, null, target);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", ok);
        resp.put("target", target);
        resp.put("message", ok ? "Test SMS dispatched successfully (Check server logs if in simulated mode)." : "Failed to dispatch SMS.");
        return ResponseEntity.ok(resp);
    }
}
