package com.aquanexus.service;

import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.WaterDailyRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates structured report data for daily, weekly, and monthly periods.
 * Supports CSV export; PDF export handled at controller layer.
 */
@Service
public class WaterReportService {

    private final WaterDailyRecordRepository recordRepository;

    public WaterReportService(WaterDailyRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * Generate report data for a given date range.
     */
    public Map<String, Object> generateReport(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<WaterDailyRecord> records = recordRepository.findByDateBetweenOrderByDateAsc(start, end);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportPeriod", startDate + " to " + endDate);
        report.put("generatedAt", LocalDate.now().toString());
        report.put("totalRecords", records.size());

        // Totals
        double totalFresh = records.stream().mapToDouble(r -> r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0).sum();
        double totalWaste = records.stream().mapToDouble(r -> r.getWastewaterGenerated() != null ? r.getWastewaterGenerated() : 0).sum();
        double totalReused = records.stream().mapToDouble(r -> r.getReusedWater() != null ? r.getReusedWater() : 0).sum();
        double totalLoss = records.stream().mapToDouble(r -> r.getWaterLoss() != null ? r.getWaterLoss() : 0).sum();
        double overallReuse = totalFresh > 0 ? Math.round((totalReused / totalFresh) * 1000.0) / 10.0 : 0;
        double avgCtoUtil = records.stream().filter(r -> r.getCtoUtilization() != null)
                .mapToDouble(WaterDailyRecord::getCtoUtilization).average().orElse(0);
        long anomalyCount = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsAnomaly())).count();

        report.put("totalFreshWaterConsumed", Math.round(totalFresh * 10.0) / 10.0);
        report.put("totalWastewaterGenerated", Math.round(totalWaste * 10.0) / 10.0);
        report.put("totalReusedWater", Math.round(totalReused * 10.0) / 10.0);
        report.put("totalWaterLoss", Math.round(totalLoss * 10.0) / 10.0);
        report.put("overallReusePercentage", overallReuse);
        report.put("averageCtoUtilization", Math.round(avgCtoUtil * 10.0) / 10.0);
        report.put("anomaliesDetected", anomalyCount);

        // Water Reuse Recommendations Summary
        Map<String, Object> recSummary = new LinkedHashMap<>();
        recSummary.put("availableReusableWater", Math.round(totalReused * 10.0) / 10.0);
        recSummary.put("potentialFreshWaterSaving", Math.round(totalReused * 10.0) / 10.0);
        recSummary.put("topRecommendedReuse", totalReused > 0 ? "Landscaping & Toilet Flushing" : "None");
        recSummary.put("qualityVerificationStatus", totalReused > 0 ? "REQUIRED" : "NOT AVAILABLE");
        report.put("waterReuseRecommendations", recSummary);

        // Department-wise summary
        Map<String, Map<String, Object>> deptSummary = new LinkedHashMap<>();
        Map<String, List<WaterDailyRecord>> byDept = records.stream()
                .filter(r -> r.getDepartment() != null)
                .collect(Collectors.groupingBy(WaterDailyRecord::getDepartment));

        for (Map.Entry<String, List<WaterDailyRecord>> entry : byDept.entrySet()) {
            Map<String, Object> dept = new LinkedHashMap<>();
            List<WaterDailyRecord> deptRecords = entry.getValue();
            double deptFresh = deptRecords.stream().mapToDouble(r -> r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0).sum();
            double deptReused = deptRecords.stream().mapToDouble(r -> r.getReusedWater() != null ? r.getReusedWater() : 0).sum();
            double deptLoss = deptRecords.stream().mapToDouble(r -> r.getWaterLoss() != null ? r.getWaterLoss() : 0).sum();
            dept.put("freshWaterConsumed", Math.round(deptFresh * 10.0) / 10.0);
            dept.put("reusedWater", Math.round(deptReused * 10.0) / 10.0);
            dept.put("waterLoss", Math.round(deptLoss * 10.0) / 10.0);
            dept.put("reusePercentage", deptFresh > 0 ? Math.round((deptReused / deptFresh) * 1000.0) / 10.0 : 0);
            dept.put("records", deptRecords.size());
            deptSummary.put(entry.getKey(), dept);
        }
        report.put("departmentSummary", deptSummary);

        // Daily breakdown
        List<Map<String, Object>> dailyData = records.stream().map(r -> {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", r.getDate().toString());
            day.put("department", r.getDepartment());
            day.put("freshWaterConsumed", r.getFreshWaterConsumed());
            day.put("wastewaterGenerated", r.getWastewaterGenerated());
            day.put("reusedWater", r.getReusedWater());
            day.put("ctoLimit", r.getCtoLimit());
            day.put("reusePercentage", r.getReusePercentage());
            day.put("ctoUtilization", r.getCtoUtilization());
            day.put("waterLoss", r.getWaterLoss());
            day.put("complianceStatus", r.getComplianceStatus());
            day.put("isAnomaly", r.getIsAnomaly());
            return day;
        }).collect(Collectors.toList());
        report.put("dailyRecords", dailyData);

        return report;
    }

    /**
     * Generate CSV string for export.
     */
    public String generateCSV(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<WaterDailyRecord> records = recordRepository.findByDateBetweenOrderByDateAsc(start, end);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Department,Industry,Fresh Water (L),Wastewater (L),Reused Water (L),CTO Limit (L),");
        csv.append("Reuse %,CTO Utilization %,Water Loss (L),Compliance,Anomaly\n");

        for (WaterDailyRecord r : records) {
            csv.append(String.format("%s,%s,%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%s,%s\n",
                    r.getDate(),
                    r.getDepartment() != null ? r.getDepartment() : "",
                    r.getIndustryName() != null ? r.getIndustryName() : "",
                    r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0,
                    r.getWastewaterGenerated() != null ? r.getWastewaterGenerated() : 0,
                    r.getReusedWater() != null ? r.getReusedWater() : 0,
                    r.getCtoLimit() != null ? r.getCtoLimit() : 0,
                    r.getReusePercentage() != null ? r.getReusePercentage() : 0,
                    r.getCtoUtilization() != null ? r.getCtoUtilization() : 0,
                    r.getWaterLoss() != null ? r.getWaterLoss() : 0,
                    r.getComplianceStatus() != null ? r.getComplianceStatus() : "",
                    Boolean.TRUE.equals(r.getIsAnomaly()) ? "YES" : "NO"));
        }
        return csv.toString();
    }
}
