package com.aquanexus.service;

import com.aquanexus.dto.DashboardSummaryDTO;
import com.aquanexus.dto.WaterRecordInputDTO;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.NotificationLogRepository;
import com.aquanexus.repository.WaterAlertRepository;
import com.aquanexus.repository.WaterDailyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business logic service for AquaNexus.
 * Computes all water metrics, evaluates CTO compliance, detects anomalies,
 * and maintains real-time data integrity based SOLELY on user-entered records.
 */
@Service
public class WaterCalculationService {

    private final WaterDailyRecordRepository recordRepository;
    private final WaterAlertRepository alertRepository;
    private final NotificationLogRepository logRepository;
    private final AlertEvaluationService alertEvaluationService;

    public WaterCalculationService(WaterDailyRecordRepository recordRepository,
                                   WaterAlertRepository alertRepository,
                                   NotificationLogRepository logRepository,
                                   AlertEvaluationService alertEvaluationService) {
        this.recordRepository = recordRepository;
        this.alertRepository = alertRepository;
        this.logRepository = logRepository;
        this.alertEvaluationService = alertEvaluationService;
    }

    // =========================================================
    // 1. SAVE NEW RECORD + COMPUTE ALL METRICS
    // =========================================================
    @Transactional
    public WaterDailyRecord saveRecord(WaterRecordInputDTO input) {
        if (input == null) {
            throw new IllegalArgumentException("Water record input data cannot be null.");
        }

        // --- Validate Physical Quantities ---
        double fresh = input.getFreshWaterConsumed() != null ? input.getFreshWaterConsumed() : 0.0;
        double waste = input.getWastewaterGenerated() != null ? input.getWastewaterGenerated() : 0.0;
        double reused = input.getReusedWater() != null ? input.getReusedWater() : 0.0;
        double ctoLimit = input.getCtoLimit() != null ? input.getCtoLimit() : 0.0;
        double treated = input.getTreatedWaterAvailable() != null ? input.getTreatedWaterAvailable() : waste;

        if (fresh < 0 || waste < 0 || reused < 0) {
            throw new IllegalArgumentException("Water quantities (fresh, wastewater, reused) cannot be negative numbers.");
        }

        // Physical validation: Reused water cannot exceed available wastewater or treated effluent
        double maxReusable = Math.max(waste, treated);
        if (reused > maxReusable && maxReusable > 0) {
            throw new IllegalArgumentException(String.format(
                    "Reused water (%.1f L) cannot exceed available wastewater / treated effluent (%.1f L). Please verify your entry.",
                    reused, maxReusable));
        }

        WaterDailyRecord record = new WaterDailyRecord();
        record.setIndustryId(input.getIndustryId() != null ? input.getIndustryId() : 1);
        record.setIndustryName(input.getIndustryName() != null ? input.getIndustryName() : "Demo Dairy Industry");
        record.setCtoNumber(input.getCtoNumber());
        record.setDepartment(input.getDepartment() != null ? input.getDepartment() : "Production");
        record.setDate(input.getDate() != null ? LocalDate.parse(input.getDate()) : LocalDate.now());
        record.setFreshWaterConsumed(Math.round(fresh * 10.0) / 10.0);
        record.setWastewaterGenerated(Math.round(waste * 10.0) / 10.0);
        record.setReusedWater(Math.round(reused * 10.0) / 10.0);
        record.setCtoLimit(ctoLimit > 0 ? Math.round(ctoLimit * 10.0) / 10.0 : null);
        record.setFacilityLocation(input.getFacilityLocation());
        record.setPlantId(input.getPlantId());
        record.setIndustryType(input.getIndustryType());
        record.setResponsibleOfficer(input.getResponsibleOfficer());
        record.setContactInfo(input.getContactInfo());
        record.setTreatedWaterAvailable(Math.round(treated * 10.0) / 10.0);

        // Meter telemetry
        double mainMeter = input.getMainMeterConsumption() != null ? input.getMainMeterConsumption() : fresh;
        double deptMeter = input.getTotalDepartmentConsumption() != null ? input.getTotalDepartmentConsumption() : fresh;
        record.setMainMeterConsumption(Math.round(mainMeter * 10.0) / 10.0);
        record.setTotalDepartmentConsumption(Math.round(deptMeter * 10.0) / 10.0);
        record.setOpeningMeterReading(input.getOpeningMeterReading());
        record.setClosingMeterReading(input.getClosingMeterReading());
        record.setMeterRecordType(input.getMeterRecordType() != null ? input.getMeterRecordType() : "Flow Meter");

        // --- Computed Metrics ---
        // 1. Reuse Percentage = (reused / fresh) * 100 (Fresh displacement rate)
        double reusePercentage = fresh > 0 ? Math.round((reused / fresh) * 1000.0) / 10.0 : 0.0;
        record.setReusePercentage(reusePercentage);

        // 2. Reuse Recovery Rate = (reused / treated) * 100
        double reuseRecoveryRate = treated > 0 ? Math.round((reused / treated) * 1000.0) / 10.0 : 0.0;
        record.setReuseRecoveryRate(reuseRecoveryRate);

        // 3. Water Loss = wastewater - reused
        double waterLoss = Math.max(0.0, Math.round((waste - reused) * 10.0) / 10.0);
        record.setWaterLoss(waterLoss);

        // 4. Unaccounted Water = mainMeter - deptMeter
        double unaccounted = Math.round((mainMeter - deptMeter) * 10.0) / 10.0;
        record.setUnaccountedWater(unaccounted);

        // 5. CTO Utilization & Compliance Status
        if (ctoLimit > 0) {
            double ctoUtil = Math.round((fresh / ctoLimit) * 1000.0) / 10.0;
            record.setCtoUtilization(ctoUtil);

            if (ctoUtil > 100.0) {
                record.setComplianceStatus("EXCEEDED");
            } else if (ctoUtil > 95.0) {
                record.setComplianceStatus("CRITICAL");
            } else if (ctoUtil >= 80.0) {
                record.setComplianceStatus("WARNING");
            } else {
                record.setComplianceStatus("SAFE");
            }
        } else {
            record.setCtoUtilization(null);
            record.setComplianceStatus("NOT_CONFIGURED");
        }

        // 6. Anomaly / Usage Variance Detection (Requires historical data baseline)
        boolean isAnomaly = detectAnomaly(record);
        record.setIsAnomaly(isAnomaly);

        // Save record
        WaterDailyRecord savedRecord = recordRepository.save(record);

        // Evaluate Immediate Rule-Based & Anomaly Alerts
        alertEvaluationService.evaluateAlertsForRecord(savedRecord);

        return savedRecord;
    }

    // =========================================================
    // 2. ANOMALY / VARIANCE DETECTION (Statistical Baseline Required)
    // =========================================================
    private boolean detectAnomaly(WaterDailyRecord current) {
        Integer industryId = current.getIndustryId() != null ? current.getIndustryId() : 1;
        List<WaterDailyRecord> history = recordRepository.findTop14ByIndustryIdOrderByDateDesc(industryId);

        // REQUIRE AT LEAST 7 HISTORICAL RECORDS for statistical baseline
        // Prevents false anomaly alerts when only 1-6 records exist in the system
        if (history.size() < 7) {
            return false;
        }

        double fresh = current.getFreshWaterConsumed() != null ? current.getFreshWaterConsumed() : 0.0;
        double avgFresh = history.stream()
                .filter(r -> !r.getId().equals(current.getId()))
                .mapToDouble(r -> r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0.0)
                .average().orElse(0.0);

        // Only flag anomaly if current fresh is > 1.5x of moving baseline AND unaccounted water is high
        if (avgFresh > 0 && fresh > avgFresh * 1.5) {
            double unacc = current.getUnaccountedWater() != null ? Math.abs(current.getUnaccountedWater()) : 0.0;
            if (unacc > 5.0) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // 3. DASHBOARD SUMMARY (Strictly Real User Records Only)
    // =========================================================
    public DashboardSummaryDTO getDashboardSummary(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        DashboardSummaryDTO dto = new DashboardSummaryDTO();

        // Fetch actual user records in timeframe
        List<WaterDailyRecord> records = recordRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);

        if (records.isEmpty()) {
            dto.setTotalFreshConsumed(0.0);
            dto.setTotalReusedWater(0.0);
            dto.setTotalWastewaterGenerated(0.0);
            dto.setTotalWaterLoss(0.0);
            dto.setFreshWaterSaved(0.0);
            dto.setOverallReusePercentage(0.0);
            dto.setOverallCtoUtilization(0.0);
            dto.setOverallComplianceStatus("NO_DATA");
            dto.setActiveAlerts(0L);
            dto.setAnomaliesDetected(0L);
            dto.setRecentAlerts(new ArrayList<>());
            dto.setDailyTrend(new ArrayList<>());
            dto.setDepartmentBreakdown(new HashMap<>());
            return dto;
        }

        // Aggregate Totals strictly from matching records
        double totalFresh = records.stream().mapToDouble(r -> r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0.0).sum();
        double totalReused = records.stream().mapToDouble(r -> r.getReusedWater() != null ? r.getReusedWater() : 0.0).sum();
        double totalWaste = records.stream().mapToDouble(r -> r.getWastewaterGenerated() != null ? r.getWastewaterGenerated() : 0.0).sum();
        double totalLoss = records.stream().mapToDouble(r -> r.getWaterLoss() != null ? r.getWaterLoss() : 0.0).sum();

        dto.setTotalFreshConsumed(Math.round(totalFresh * 10.0) / 10.0);
        dto.setTotalReusedWater(Math.round(totalReused * 10.0) / 10.0);
        dto.setTotalWastewaterGenerated(Math.round(totalWaste * 10.0) / 10.0);
        dto.setTotalWaterLoss(Math.round(totalLoss * 10.0) / 10.0);
        dto.setFreshWaterSaved(Math.round(totalReused * 10.0) / 10.0);

        // Overall Reuse Percentage
        double overallReuse = totalFresh > 0 ? Math.round((totalReused / totalFresh) * 1000.0) / 10.0 : 0.0;
        dto.setOverallReusePercentage(overallReuse);

        // Overall CTO Utilization (Average across records that have a configured CTO limit)
        List<WaterDailyRecord> recordsWithCto = records.stream()
                .filter(r -> r.getCtoUtilization() != null)
                .collect(Collectors.toList());

        if (!recordsWithCto.isEmpty()) {
            double avgCtoUtil = recordsWithCto.stream()
                    .mapToDouble(WaterDailyRecord::getCtoUtilization)
                    .average().orElse(0.0);
            dto.setOverallCtoUtilization(Math.round(avgCtoUtil * 10.0) / 10.0);

            if (avgCtoUtil > 100.0) {
                dto.setOverallComplianceStatus("EXCEEDED");
            } else if (avgCtoUtil > 95.0) {
                dto.setOverallComplianceStatus("CRITICAL");
            } else if (avgCtoUtil >= 80.0) {
                dto.setOverallComplianceStatus("WARNING");
            } else {
                dto.setOverallComplianceStatus("SAFE");
            }
        } else {
            dto.setOverallCtoUtilization(0.0);
            dto.setOverallComplianceStatus("NOT_CONFIGURED");
        }

        // Active Alerts count and anomalies
        dto.setActiveAlerts(alertRepository.countByIsResolvedFalse());
        long anomalies = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsAnomaly())).count();
        dto.setAnomaliesDetected(anomalies);

        // Recent Active Alerts
        List<WaterAlert> recentAlerts = alertRepository.findByIsResolvedFalseOrderByCreatedAtDesc();
        List<DashboardSummaryDTO.AlertDTO> alertDTOs = recentAlerts.stream()
                .limit(10)
                .map(a -> new DashboardSummaryDTO.AlertDTO(
                        a.getDate() != null ? a.getDate().toString() : "",
                        a.getAlertType() != null ? a.getAlertType() : "ALERT",
                        a.getSeverity() != null ? a.getSeverity() : "INFO",
                        a.getMessage() != null ? a.getMessage() : ""))
                .collect(Collectors.toList());
        dto.setRecentAlerts(alertDTOs);

        // Daily Trend
        List<DashboardSummaryDTO.DailyTrendDTO> trend = records.stream()
                .map(r -> new DashboardSummaryDTO.DailyTrendDTO(
                        r.getDate() != null ? r.getDate().toString() : "",
                        r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0.0,
                        r.getReusedWater() != null ? r.getReusedWater() : 0.0,
                        r.getWaterLoss() != null ? r.getWaterLoss() : 0.0,
                        r.getReusePercentage() != null ? r.getReusePercentage() : 0.0))
                .collect(Collectors.toList());
        dto.setDailyTrend(trend);

        // Department Breakdown
        Map<String, Double> deptBreakdown = records.stream()
                .filter(r -> r.getDepartment() != null && r.getFreshWaterConsumed() != null)
                .collect(Collectors.groupingBy(
                        WaterDailyRecord::getDepartment,
                        Collectors.summingDouble(WaterDailyRecord::getFreshWaterConsumed)));
        dto.setDepartmentBreakdown(deptBreakdown);

        return dto;
    }

    // =========================================================
    // 4. FETCH ALL RECORDS (with filters)
    // =========================================================
    public List<WaterDailyRecord> getRecords(String startDate, String endDate, String department) {
        LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : LocalDate.now();

        if (department != null && !department.isBlank() && !"ALL".equalsIgnoreCase(department)) {
            return recordRepository.findByDepartmentAndDateBetweenOrderByDateAsc(department, start, end);
        }
        return recordRepository.findByDateBetweenOrderByDateAsc(start, end);
    }

    // =========================================================
    // 5. DELETE RECORD BY ID
    // =========================================================
    @Transactional
    public boolean deleteRecord(Long id) {
        if (recordRepository.existsById(id)) {
            // Also clean up any associated alerts
            List<WaterAlert> relatedAlerts = alertRepository.findByRecordId(id);
            if (!relatedAlerts.isEmpty()) {
                alertRepository.deleteAll(relatedAlerts);
            }
            recordRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // =========================================================
    // 6. RESET ALL DATA (Clean State Utility)
    // =========================================================
    @Transactional
    public void resetAllData() {
        alertRepository.deleteAll();
        logRepository.deleteAll();
        recordRepository.deleteAll();
    }

    // =========================================================
    // 7. GET LATEST SAVED COMPANY PROFILE
    // =========================================================
    public Map<String, String> getLatestCompanyProfile() {
        List<WaterDailyRecord> records = recordRepository.findAllOrderByDateDesc();
        Map<String, String> profile = new LinkedHashMap<>();
        if (!records.isEmpty()) {
            WaterDailyRecord r = records.get(0);
            profile.put("companyName", r.getIndustryName() != null ? r.getIndustryName() : "");
            profile.put("ctoNumber", r.getCtoNumber() != null ? r.getCtoNumber() : "");
            profile.put("facilityLocation", r.getFacilityLocation() != null ? r.getFacilityLocation() : "");
            profile.put("plantId", r.getPlantId() != null ? r.getPlantId() : "");
            profile.put("industryType", r.getIndustryType() != null ? r.getIndustryType() : "");
            profile.put("responsibleOfficer", r.getResponsibleOfficer() != null ? r.getResponsibleOfficer() : "");
            profile.put("contactInfo", r.getContactInfo() != null ? r.getContactInfo() : "");
        } else {
            profile.put("companyName", "");
            profile.put("ctoNumber", "");
            profile.put("facilityLocation", "");
            profile.put("plantId", "");
            profile.put("industryType", "");
            profile.put("responsibleOfficer", "");
            profile.put("contactInfo", "");
        }
        return profile;
    }
}
