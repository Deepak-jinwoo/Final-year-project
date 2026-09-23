package com.aquanexus.service;

import com.aquanexus.dto.DashboardSummaryDTO;
import com.aquanexus.dto.WaterReuseRecommendationDTO;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.WaterDailyRecordRepository;
import com.aquanexus.repository.WaterAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Domain-Specific AI Assistant Service for AquaNexus.
 * Combines real database metrics, CTO compliance, anomalies, and ML forecasts
 * with LLM intelligence (via NVIDIA NIM API) to answer user queries dynamically.
 */
@Service
public class AIAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantService.class);

    private final WaterDailyRecordRepository recordRepository;
    private final WaterAlertRepository alertRepository;
    private final WaterCalculationService calculationService;
    private final WaterReuseRecommendationService recommendationService;
    private final RestTemplate restTemplate;

    @Value("${app.ai.nvidia.api-key:}")
    private String nvidiaApiKey;

    @Value("${app.ai.nvidia.base-url:https://integrate.api.nvidia.com/v1}")
    private String nvidiaBaseUrl;

    @Value("${app.ai.nvidia.model:openai/gpt-oss-20b}")
    private String nvidiaModel;

    public AIAssistantService(WaterDailyRecordRepository recordRepository,
                              WaterAlertRepository alertRepository,
                              WaterCalculationService calculationService,
                              WaterReuseRecommendationService recommendationService) {
        this.recordRepository = recordRepository;
        this.alertRepository = alertRepository;
        this.calculationService = calculationService;
        this.recommendationService = recommendationService;
        this.restTemplate = new RestTemplate();
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

        // Build context for LLM
        String systemContext = String.format(
            "You are AquaNexus AI, an enterprise industrial water management assistant.\n" +
            "Answer user questions accurately using these real database metrics:\n" +
            "- Total Records: %d\n" +
            "- Total Fresh Water Consumed: %.1f L\n" +
            "- Total Wastewater Generated: %.1f L\n" +
            "- Total Water Reused: %.1f L\n" +
            "- Overall Reuse Percentage: %.1f%%\n" +
            "- Total Water Loss: %.1f L\n" +
            "- Overall CTO Utilization: %.1f%% (Status: %s)\n" +
            "- Active Alerts: %d, Anomalies Detected: %d\n" +
            "- Latest Record Department: %s, Date: %s, Fresh Consumed: %.1f L, Reused: %.1f L\n" +
            "Respond concisely with markdown formatting.",
            recordCount, totalFresh, summary.getTotalWastewaterGenerated(), totalReused,
            reusePct, totalLoss, ctoUtil, ctoStatus, activeAlerts, anomalies,
            latest.getDepartment() != null ? latest.getDepartment() : "Production",
            latest.getDate() != null ? latest.getDate().toString() : "Today",
            latest.getFreshWaterConsumed() != null ? latest.getFreshWaterConsumed() : 0.0,
            latest.getReusedWater() != null ? latest.getReusedWater() : 0.0
        );

        // Try calling NVIDIA NIM LLM API if key is present
        if (nvidiaApiKey != null && !nvidiaApiKey.isBlank()) {
            try {
                String llmAnswer = callNvidiaLLM(prompt, systemContext);
                if (llmAnswer != null && !llmAnswer.isBlank()) {
                    response.put("success", true);
                    response.put("intent", "LLM_RESPONSE");
                    response.put("message", llmAnswer);
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
            } catch (Exception e) {
                log.warn("NVIDIA NIM API call failed, falling back to domain logic: {}", e.getMessage());
            }
        }

        StringBuilder answer = new StringBuilder();
        String intent = "GENERAL_INQUIRY";

        // Intent Classification & Response Generation (Fallback)
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

    @SuppressWarnings("unchecked")
    private String callNvidiaLLM(String userPrompt, String systemContext) {
        String url = nvidiaBaseUrl.endsWith("/") ? nvidiaBaseUrl + "chat/completions" : nvidiaBaseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(nvidiaApiKey.trim());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", nvidiaModel != null && !nvidiaModel.isBlank() ? nvidiaModel.trim() : "openai/gpt-oss-20b");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemContext));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.5);
        requestBody.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null && message.get("content") != null) {
                    return message.get("content").toString();
                }
            }
        }
        return null;
    }
}
