package com.aquanexus.service;

import com.aquanexus.model.NotificationSetting;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.NotificationSettingRepository;
import com.aquanexus.repository.WaterAlertRepository;
import com.aquanexus.repository.WaterDailyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Enterprise Alert Evaluation Engine.
 * Evaluates immediate rule-based and anomaly alerts based SOLELY on actual user records.
 * Strictly adheres to non-hardware wording: "Possible abnormal water usage / possible unaccounted water detected."
 */
@Service
public class AlertEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationService.class);

    private final WaterAlertRepository alertRepository;
    private final WaterDailyRecordRepository recordRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationDispatcherService dispatcherService;

    public AlertEvaluationService(WaterAlertRepository alertRepository,
                                  WaterDailyRecordRepository recordRepository,
                                  NotificationSettingRepository settingRepository,
                                  NotificationDispatcherService dispatcherService) {
        this.alertRepository = alertRepository;
        this.recordRepository = recordRepository;
        this.settingRepository = settingRepository;
        this.dispatcherService = dispatcherService;
    }

    /**
     * Evaluates all compliance and usage alert rules immediately for a saved/updated user record.
     */
    public void evaluateAlertsForRecord(WaterDailyRecord record) {
        if (record == null) return;

        Integer industryId = record.getIndustryId() != null ? record.getIndustryId() : 1;
        NotificationSetting setting = settingRepository.findByIndustryId(industryId)
                .orElseGet(() -> {
                    NotificationSetting s = new NotificationSetting();
                    s.setIndustryId(industryId);
                    return settingRepository.save(s);
                });

        double fresh = record.getFreshWaterConsumed() != null ? record.getFreshWaterConsumed() : 0.0;
        double waste = record.getWastewaterGenerated() != null ? record.getWastewaterGenerated() : 0.0;
        double reused = record.getReusedWater() != null ? record.getReusedWater() : 0.0;
        double treated = record.getTreatedWaterAvailable() != null ? record.getTreatedWaterAvailable() : waste;
        Double ctoLimit = record.getCtoLimit();
        double unaccounted = record.getUnaccountedWater() != null ? record.getUnaccountedWater() : 0.0;
        double recoveryRate = record.getReuseRecoveryRate() != null ? record.getReuseRecoveryRate() : (treated > 0 ? (reused / treated) * 100 : 0.0);

        // =========================================================
        // 1. RULE A: CTO COMPLIANCE ALERTS (Only if CTO Limit is set)
        // =========================================================
        if (ctoLimit != null && ctoLimit > 0) {
            double ctoUtil = (fresh / ctoLimit) * 100.0;
            if (ctoUtil > 100.0) {
                String title = "CTO Daily Limit Exceeded";
                String msg = String.format("CTO daily limit exceeded at %.1f%% (Fresh: %,.1f L / Limit: %,.1f L) in %s on %s.",
                        ctoUtil, fresh, ctoLimit, record.getDepartment(), record.getDate());
                String exp = String.format("Water abstraction has exceeded the consented regulatory limit by %,.1f L. Immediate operational review and recycling required to avoid compliance action.",
                        fresh - ctoLimit);
                processAlert(record, "CTO_EXCEEDED", "CRITICAL", title, msg, exp);
            } else if (ctoUtil > 95.0) {
                String title = "CTO Near Limit Exhaustion";
                String msg = String.format("CTO utilization reached %.1f%% in %s on %s. Approaching daily ceiling of %,.1f L.",
                        ctoUtil, record.getDepartment(), record.getDate(), ctoLimit);
                String exp = "Water abstraction has reached critical capacity (>95%). Switch secondary processes to recycled water lines to prevent breach.";
                processAlert(record, "CTO_CRITICAL", "CRITICAL", title, msg, exp);
            } else if (ctoUtil >= 80.0) {
                String title = "CTO Approaching Warning Threshold";
                String msg = String.format("CTO utilization at %.1f%% for %s on %s. Consumed %,.1f L of %,.1f L limit.",
                        ctoUtil, record.getDepartment(), record.getDate(), fresh, ctoLimit);
                String exp = "Water abstraction is in the warning band (80%-95%). Monitor department meters closely.";
                processAlert(record, "CTO_WARNING", "WARNING", title, msg, exp);
            }
        }

        // =========================================================
        // 2. RULE B: UNACCOUNTED WATER ALERT
        // =========================================================
        double unaccThresholdPct = setting.getUnaccountedWaterThresholdPct() != null ? setting.getUnaccountedWaterThresholdPct() : 3.0;
        double unaccThresholdKl = setting.getUnaccountedWaterThresholdKl() != null ? setting.getUnaccountedWaterThresholdKl() : 5.0;
        double absUnaccounted = Math.abs(unaccounted);
        double unaccPct = fresh > 0 ? (absUnaccounted / fresh) * 100.0 : 0.0;

        // Unaccounted water alert only when difference exceeds configured bounds and fresh > 0
        if (fresh > 0 && (absUnaccounted > unaccThresholdKl || unaccPct > unaccThresholdPct)) {
            String severity = (absUnaccounted > 15.0 || unaccPct > 10.0) ? "CRITICAL" : "WARNING";
            String title = "Possible Unaccounted Water Detected";
            String msg = String.format("Unaccounted water of %,.1f L detected. Verify meter readings, unmetered use, timing differences, calibration, and possible leakage.",
                    absUnaccounted);
            String exp = String.format("Main flow meter reading (%,.1f L) differs from department meters by %,.1f L (%.1f%% of abstraction). Requires operational investigation.",
                    record.getMainMeterConsumption() != null ? record.getMainMeterConsumption() : fresh,
                    absUnaccounted, unaccPct);
            processAlert(record, "UNACCOUNTED_WATER", severity, title, msg, exp);
        }

        // =========================================================
        // 3. RULE C: LOW WATER REUSE RECOVERY
        // =========================================================
        double targetRecovery = setting.getLowReuseTargetPct() != null ? setting.getLowReuseTargetPct() : 60.0;
        if (treated >= 50.0 && recoveryRate < targetRecovery) {
            String title = "Low Water Reuse Recovery Rate";
            String msg = String.format("Water reuse recovery rate is %.1f%% (target: %.1f%%). Actual reused: %,.1f L, Treated available: %,.1f L.",
                    recoveryRate, targetRecovery, reused, treated);
            String exp = String.format("Treated effluent is available (%,.1f L) but underutilized. Direct treated water to eligible washing or cooling towers.",
                    treated);
            processAlert(record, "LOW_REUSE", "WARNING", title, msg, exp);
        }

        // =========================================================
        // 4. RULE D: UNTAPPED REUSE POTENTIAL
        // =========================================================
        double untappedThreshold = setting.getUntappedReuseThresholdKl() != null ? setting.getUntappedReuseThresholdKl() : 50.0;
        double untappedPotential = Math.max(0.0, treated - reused);
        if (untappedPotential > untappedThreshold && fresh > 50.0) {
            String title = "High Untapped Water Reuse Potential";
            String msg = String.format("Untapped reuse potential of %,.1f L available in %s. Reused %,.1f L out of %,.1f L treated effluent.",
                    untappedPotential, record.getDepartment(), reused, treated);
            String exp = "Significant volume of approved treated effluent is being discharged unused instead of displacing fresh water.";
            processAlert(record, "UNTAPPED_REUSE", "INFO", title, msg, exp);
        }

        // =========================================================
        // 5. RULE E: ABNORMAL USAGE / VARIANCE DETECTION
        // =========================================================
        boolean isAnomaly = Boolean.TRUE.equals(record.getIsAnomaly());
        if (isAnomaly) {
            List<WaterDailyRecord> history = recordRepository.findTop14ByIndustryIdOrderByDateDesc(industryId);
            // Require at least 7 historical records for statistical variance evaluation
            if (history.size() >= 7) {
                double avgFresh = history.stream()
                        .filter(r -> !r.getId().equals(record.getId()))
                        .mapToDouble(r -> r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0.0)
                        .average().orElse(fresh);

                double variancePct = avgFresh > 0 ? ((fresh - avgFresh) / avgFresh) * 100.0 : 0.0;
                String severity = (fresh > avgFresh * 1.8) ? "CRITICAL" : "WARNING";

                String title = "Possible abnormal water usage detected";
                String msg = "Water usage differs significantly from recent historical records. Verify meter readings, department logs, and water lines.";
                String exp = String.format("Main-meter consumption is %.1f%% higher than the previous 7-day average (%,.1f L vs baseline %,.1f L), and unaccounted water is %,.1f L.",
                        variancePct, fresh, avgFresh, absUnaccounted);

                processAlert(record, "ABNORMAL_USAGE", severity, title, msg, exp);
            }
        }
    }

    /**
     * Deduplicates and handles escalation of alerts.
     */
    private void processAlert(WaterDailyRecord record, String alertType, String severity, String title, String msg, String exp) {
        Integer industryId = record.getIndustryId() != null ? record.getIndustryId() : 1;
        String dept = record.getDepartment();
        LocalDate date = record.getDate();

        // Check for existing duplicate alert
        Optional<WaterAlert> existingOpt = alertRepository.findFirstByIndustryIdAndDepartmentAndDateAndAlertTypeOrderByCreatedAtDesc(
                industryId, dept, date, alertType);

        if (existingOpt.isPresent()) {
            WaterAlert existing = existingOpt.get();
            String oldSeverity = existing.getSeverity() != null ? existing.getSeverity() : "INFO";

            boolean isEscalation = isHigherSeverity(severity, oldSeverity);
            boolean wasNeverNotified = !Boolean.TRUE.equals(existing.getNotificationSent());

            existing.setTitle(title);
            existing.setMessage(msg);
            existing.setExplanation(exp);
            existing.setSeverity(severity);
            existing.setRecordId(record.getId());
            existing.setUpdatedAt(LocalDateTime.now());
            alertRepository.save(existing);

            if (isEscalation || wasNeverNotified) {
                log.info("Dispatching escalated notification for alert {} ({})", existing.getId(), alertType);
                dispatcherService.dispatchAlertNotification(existing, record);
            }
        } else {
            // New Alert Creation
            WaterAlert alert = new WaterAlert();
            alert.setIndustryId(industryId);
            alert.setDepartment(dept);
            alert.setRecordId(record.getId());
            alert.setDate(date);
            alert.setAlertType(alertType);
            alert.setSeverity(severity);
            alert.setTitle(title);
            alert.setMessage(msg);
            alert.setExplanation(exp);
            alert.setStatus("OPEN");
            alert.setIsResolved(false);

            WaterAlert savedAlert = alertRepository.save(alert);
            log.info("New alert created: ID={} Type={} Severity={}", savedAlert.getId(), alertType, severity);

            // Dispatch notification
            dispatcherService.dispatchAlertNotification(savedAlert, record);
        }
    }

    private boolean isHigherSeverity(String newSev, String oldSev) {
        if ("CRITICAL".equalsIgnoreCase(newSev) && !"CRITICAL".equalsIgnoreCase(oldSev)) return true;
        if ("WARNING".equalsIgnoreCase(newSev) && "INFO".equalsIgnoreCase(oldSev)) return true;
        return false;
    }
}
