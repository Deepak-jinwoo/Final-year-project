package com.aquanexus.service;

import com.aquanexus.dto.DashboardSummaryDTO;
import com.aquanexus.dto.WaterReuseRecommendationDTO;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.WaterDailyRecordRepository;
import com.aquanexus.repository.WaterAlertRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Domain-Specific AI Assistant Service for AquaNexus.
 * Analyzes real database metrics, CTO compliance, anomalies, and ML forecasts
 * to answer user queries strictly with actual user-entered data. Does NOT invent values.
 */
@Service
public class AIAssistantService {

    private final WaterDailyRecordRepository recordRepository;
    private final WaterAlertRepository alertRepository;
    private final WaterCalculationService calculationService;
    private final WaterReuseRecommendationService recommendationService;

    public AIAssistantService(WaterDailyRecordRepository recordRepository,
                              WaterAlertRepository alertRepository,
                              WaterCalculationService calculationService,
                              WaterReuseRecommendationService recommendationService) {
        this.recordRepository = recordRepository;
        this.alertRepository = alertRepository;
        this.calculationService = calculationService;
        this.recommendationService = recommendationService;
    }

    public Map<String, Object> processUserPrompt(String prompt) {
        Map<String, Object> response = new LinkedHashMap<>();
        String query = prompt != null ? prompt.toLowerCase().trim() : "";

        DashboardSummaryDTO summary = calculationService.getDashboardSummary(30);
        List<WaterDailyRecord> records = recordRepository.findAllOrderByDateDesc();

        // Empty State Check
        if (records.isEmpty()) {
            response.put("success", true);
            response.put("intent", "EMPTY_STATE");
            response.put("message", "### 💧 AquaNexus AI Assistant Notice\n\nNo water usage records are currently available in the database.\n\nPlease navigate to **[Consumption Records](#/consumption)** and submit a daily entry to begin real-time AI monitoring and CTO compliance tracking.");
            response.put("metrics", Map.of("hasData", false));
            return response;
        }

        WaterDailyRecord latest = records.get(0);
        int recordCount = records.size();
        double totalFresh = summary.getTotalFreshConsumed();
        double totalReused = summary.getTotalReusedWater();
        double totalLoss = summary.getTotalWaterLoss();
        double reusePct = summary.getOverallReusePercentage();
        double ctoUtil = summary.getOverallCtoUtilization();
        String ctoStatus = summary.getOverallComplianceStatus();
        long activeAlerts = summary.getActiveAlerts();
        long anomalies = summary.getAnomaliesDetected();

        StringBuilder answer = new StringBuilder();
        String intent = "GENERAL_INQUIRY";

        // Intent Classification & Response Generation
        if (query.contains("usage") || query.contains("consumed") || query.contains("current water") || query.contains("how much water") || query.contains("today")) {
            intent = "WATER_USAGE";
            answer.append("### 📊 Actual Water Consumption Analysis\n\n");
            if (recordCount == 1) {
                answer.append("Based on **1 recorded daily entry**:\n\n");
            } else {
                answer.append(String.format("Based on **%d recorded entries** in the active dataset:\n\n", recordCount));
            }
            answer.append(String.format("- **Total Fresh Water Consumed:** `%.1f L`\n", totalFresh));
            answer.append(String.format("- **Total Wastewater Generated:** `%.1f L`\n", summary.getTotalWastewaterGenerated()));
            answer.append(String.format("- **Total Water Reused:** `%.1f L`\n", totalReused));
            answer.append(String.format("- **Overall Water Loss:** `%.1f L`\n", totalLoss));
            answer.append(String.format("- **Reuse Rate:** `%.1f%%`\n\n", reusePct));

            if (latest.getDepartment() != null) {
                answer.append(String.format("Latest entry from **%s** on `%s`: `%.1f L` fresh water consumed, `%.1f L` reused.\n",
                        latest.getDepartment(), latest.getDate(), latest.getFreshWaterConsumed(), latest.getReusedWater()));
            }

        } else if (query.contains("cto") || query.contains("compliance") || query.contains("warning") || query.contains("critical") || query.contains("limit") || query.contains("exceed")) {
            intent = "CTO_COMPLIANCE";
            answer.append("### 🛡️ CTO Compliance Status\n\n");
            if ("NOT_CONFIGURED".equals(ctoStatus)) {
                answer.append("ℹ️ **CTO Limit Not Configured:** No CTO daily limit has been set on current records.\n\n");
            } else {
                answer.append(String.format("Your current calculated **CTO Utilization is %.1f%%**, classifying status as **%s**.\n\n", ctoUtil, ctoStatus));

                if (ctoStatus.equals("SAFE")) {
                    answer.append("✅ **Safe Status (< 80%):** Water consumption is operating safely within the consented daily abstraction limit.\n\n");
                } else if (ctoStatus.equals("WARNING")) {
                    answer.append("⚠️ **Warning Status (80% - 95%):** Approaching maximum daily Consent to Operate threshold.\n\n");
                } else if (ctoStatus.equals("CRITICAL")) {
                    answer.append("🚨 **Critical Status (95% - 100%):** Limit near exhaustion. Reduce intake immediately.\n\n");
                } else {
                    answer.append("🚨 **Limit Exceeded (> 100%):** Consented CTO daily limit has been breached!\n\n");
                }
            }

            answer.append(String.format("- **Active Unresolved Alerts:** `%d`\n", activeAlerts));
            if (latest.getCtoLimit() != null && latest.getCtoLimit() > 0) {
                answer.append(String.format("- **Latest Recorded CTO Limit:** `%.1f L/day`\n", latest.getCtoLimit()));
            }

        } else if (query.contains("anomaly") || query.contains("anomalies") || query.contains("leak") || query.contains("isolation forest")) {
            intent = "ANOMALY_DETECTION";
            answer.append("### 🔍 Anomaly & Usage Variance Analysis\n\n");
            if (recordCount < 7) {
                answer.append(String.format("ℹ️ **Insufficient Data for Anomaly Detection:** The system requires at least 7 historical daily entries to calculate statistical baselines. Currently, only `%d` record(s) exist, so no abnormal usage pattern is claimed.\n", recordCount));
            } else {
                answer.append(String.format("The AI anomaly detection system has evaluated %d historical records and identified **%d unusual usage pattern(s)**.\n\n", recordCount, anomalies));
                if (anomalies > 0) {
                    answer.append("👉 Check the **[Alerts Center](#/alerts)** or **[Consumption Table](#/consumption)** to inspect flagged records.");
                } else {
                    answer.append("No abnormal usage patterns detected. Daily abstraction matches expected baselines.");
                }
            }

        } else if (query.contains("reuse") || query.contains("recycle") || query.contains("suggest") || query.contains("recommend")) {
            intent = "WATER_REUSE";
            WaterReuseRecommendationDTO recs = recommendationService.getRecommendations();

            answer.append("### ♻️ Water Reuse & Conservation Recommendations\n\n");
            answer.append(String.format("Based on your recorded reusable water volume of **`%.1f L`**, the following non-potable applications are evaluated:\n\n", recs.getAvailableReusableWater()));

            if (recs.getRecommendations() != null && !recs.getRecommendations().isEmpty()) {
                for (var r : recs.getRecommendations()) {
                    answer.append(String.format("- **%s** (`%s`): Allocation of `%.1f L/day` (Fresh saving: `%.1f L/day`). *%s*\n",
                            r.getApplicationName(), r.getStatus(), r.getPotentialReuseQuantity(), r.getPotentialFreshSaving(), r.getReason()));
                }
            }
            answer.append("\n⚠️ *Safety Rule:* Water quality verification is required before operational deployment.");

        } else if (query.contains("department") || query.contains("dept") || query.contains("highest") || query.contains("most")) {
            intent = "DEPARTMENT_BREAKDOWN";
            answer.append("### 🏢 Department Consumption Breakdown\n\n");
            Map<String, Double> depts = summary.getDepartmentBreakdown();
            if (depts != null && !depts.isEmpty()) {
                String topDept = "";
                double topVol = -1;
                for (Map.Entry<String, Double> entry : depts.entrySet()) {
                    answer.append(String.format("- **%s:** `%.1f L`\n", entry.getKey(), entry.getValue()));
                    if (entry.getValue() > topVol) {
                        topVol = entry.getValue();
                        topDept = entry.getKey();
                    }
                }
                answer.append(String.format("\n**Highest consuming department:** `%s` with `%.1f L` fresh water consumed.\n", topDept, topVol));
            } else {
                answer.append("No department breakdown data available.");
            }

        } else if (query.contains("alert") || query.contains("alerts") || query.contains("notification")) {
            intent = "ALERTS_SUMMARY";
            answer.append("### 🔔 Active Alerts Summary\n\n");
            answer.append(String.format("- **Active Unresolved Alerts:** `%d`\n", activeAlerts));
            if (summary.getRecentAlerts() != null && !summary.getRecentAlerts().isEmpty()) {
                answer.append("\n**Recent Events:**\n");
                for (var a : summary.getRecentAlerts()) {
                    answer.append(String.format("- `[%s]` **%s** (%s): %s\n", a.getDate(), a.getAlertType(), a.getSeverity(), a.getMessage()));
                }
            } else {
                answer.append("✅ No active alerts at this time.");
            }

        } else {
            // General Overview
            intent = "GENERAL_SUMMARY";
            answer.append("### 💧 AquaNexus System Intelligence Summary\n\n");
            answer.append(String.format("Currently tracking **%d user-entered record(s)**:\n\n", recordCount));
            answer.append(String.format("- **Total Fresh Consumed:** `%.1f L`\n", totalFresh));
            answer.append(String.format("- **Total Reused:** `%.1f L` (Reuse Rate: `%.1f%%`)\n", totalReused, reusePct));
            answer.append(String.format("- **CTO Status:** `%s` (Utilization: `%.1f%%`)\n", ctoStatus, ctoUtil));
            answer.append(String.format("- **Active Alerts:** `%d`\n\n", activeAlerts));
            answer.append("Ask me specific questions about water usage, CTO limits, alerts, departments, or recycling recommendations.");
        }

        response.put("success", true);
        response.put("intent", intent);
        response.put("message", answer.toString());
        response.put("metrics", Map.of(
                "hasData", true,
                "recordCount", recordCount,
                "totalFreshConsumed", totalFresh,
                "totalReusedWater", totalReused,
                "overallCtoUtilization", ctoUtil,
                "overallComplianceStatus", ctoStatus
        ));

        return response;
    }
}
