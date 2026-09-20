package com.aquanexus.controller;

import com.aquanexus.model.NotificationSetting;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.repository.NotificationSettingRepository;
import com.aquanexus.repository.WaterAlertRepository;
import com.aquanexus.service.EmailNotificationService;
import com.aquanexus.service.WaterMonthlyReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for automated and manual monthly report generation and email dispatch.
 */
@RestController
@RequestMapping("/api/reports/monthly")
public class MonthlyReportController {

    private final WaterMonthlyReportService monthlyReportService;
    private final EmailNotificationService emailService;
    private final NotificationSettingRepository settingRepository;
    private final WaterAlertRepository alertRepository;

    public MonthlyReportController(WaterMonthlyReportService monthlyReportService,
                                   EmailNotificationService emailService,
                                   NotificationSettingRepository settingRepository,
                                   WaterAlertRepository alertRepository) {
        this.monthlyReportService = monthlyReportService;
        this.emailService = emailService;
        this.settingRepository = settingRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * POST /api/reports/monthly/generate
     * Manually triggers generation of the monthly report for a specific year/month.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateMonthlyReport(
            @RequestParam(required = false, defaultValue = "1") Integer industryId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        String periodName = ym.getMonth().toString() + " " + ym.getYear();

        var reportEntity = monthlyReportService.generateMonthlyReport(start, end, periodName);

        // Record a MONTHLY_COMPLIANCE_SUMMARY alert/info event
        WaterAlert alert = new WaterAlert();
        alert.setIndustryId(industryId);
        alert.setDate(end);
        alert.setAlertType("MONTHLY_COMPLIANCE_SUMMARY");
        alert.setSeverity("INFO");
        alert.setTitle("Monthly Compliance Report Generated — " + periodName);
        alert.setMessage("Monthly compliance audit report has been compiled and archived.");
        alert.setExplanation("Monthly report covers CTO utilization, total abstraction, recycling yield, and unresolved anomalies.");
        alert.setStatus("OPEN");
        alert.setIsResolved(false);
        alertRepository.save(alert);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("period", periodName);
        resp.put("reportId", reportEntity != null ? reportEntity.getId() : null);
        resp.put("message", "Monthly report compiled successfully.");
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/reports/monthly/send
     * Generates and emails the monthly report PDF to registered recipients.
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMonthlyReport(
            @RequestParam(required = false, defaultValue = "1") Integer industryId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String recipientEmail) {

        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        String periodName = ym.getMonth().toString() + " " + ym.getYear();

        var reportEntity = monthlyReportService.generateMonthlyReport(start, end, periodName);
        byte[] pdfBytes = (reportEntity != null) ? reportEntity.getPdfData() : null;

        NotificationSetting setting = settingRepository.findByIndustryId(industryId).orElse(new NotificationSetting());
        String targetEmail = (recipientEmail != null && !recipientEmail.isEmpty()) ? recipientEmail : setting.getRegisteredEmail();

        boolean sent = emailService.sendMonthlyReportEmail(targetEmail, periodName, pdfBytes);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", sent);
        resp.put("recipient", targetEmail);
        resp.put("period", periodName);
        resp.put("message", sent ? "Monthly report email dispatched successfully." : "Failed to dispatch email (check recipient address).");
        return ResponseEntity.ok(resp);
    }
}
