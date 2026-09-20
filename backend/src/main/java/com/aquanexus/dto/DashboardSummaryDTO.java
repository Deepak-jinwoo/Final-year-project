package com.aquanexus.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO returned to the frontend for dynamic dashboard rendering.
 * Every dashboard card binds to a field in this response.
 */
public class DashboardSummaryDTO {

    // === Top Metric Cards ===
    private double totalFreshConsumed;
    private double totalReusedWater;
    private double totalWastewaterGenerated;
    private double totalWaterLoss;
    private double overallReusePercentage;
    private double overallCtoUtilization;
    private double freshWaterSaved;   // totalReusedWater (water that didn't need to be fresh)
    private String overallComplianceStatus;  // SAFE, WARNING, CRITICAL

    // === Alert Summary ===
    private long activeAlerts;
    private long anomaliesDetected;
    private List<AlertDTO> recentAlerts;

    // === Trend Data (for charts) ===
    private List<DailyTrendDTO> dailyTrend;        // 7-day or 30-day trend
    private Map<String, Double> departmentBreakdown; // department -> total fresh consumed

    // === ML Predictions (from XGBoost) ===
    private Double predictedFreshWater;
    private Double predictedReusedWater;
    private String predictionConfidence;

    // Getters & Setters
    public double getTotalFreshConsumed() { return totalFreshConsumed; }
    public void setTotalFreshConsumed(double v) { this.totalFreshConsumed = v; }

    public double getTotalReusedWater() { return totalReusedWater; }
    public void setTotalReusedWater(double v) { this.totalReusedWater = v; }

    public double getTotalWastewaterGenerated() { return totalWastewaterGenerated; }
    public void setTotalWastewaterGenerated(double v) { this.totalWastewaterGenerated = v; }

    public double getTotalWaterLoss() { return totalWaterLoss; }
    public void setTotalWaterLoss(double v) { this.totalWaterLoss = v; }

    public double getOverallReusePercentage() { return overallReusePercentage; }
    public void setOverallReusePercentage(double v) { this.overallReusePercentage = v; }

    public double getOverallCtoUtilization() { return overallCtoUtilization; }
    public void setOverallCtoUtilization(double v) { this.overallCtoUtilization = v; }

    public double getFreshWaterSaved() { return freshWaterSaved; }
    public void setFreshWaterSaved(double v) { this.freshWaterSaved = v; }

    public String getOverallComplianceStatus() { return overallComplianceStatus; }
    public void setOverallComplianceStatus(String v) { this.overallComplianceStatus = v; }

    public long getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(long v) { this.activeAlerts = v; }

    public long getAnomaliesDetected() { return anomaliesDetected; }
    public void setAnomaliesDetected(long v) { this.anomaliesDetected = v; }

    public List<AlertDTO> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<AlertDTO> v) { this.recentAlerts = v; }

    public List<DailyTrendDTO> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<DailyTrendDTO> v) { this.dailyTrend = v; }

    public Map<String, Double> getDepartmentBreakdown() { return departmentBreakdown; }
    public void setDepartmentBreakdown(Map<String, Double> v) { this.departmentBreakdown = v; }

    public Double getPredictedFreshWater() { return predictedFreshWater; }
    public void setPredictedFreshWater(Double v) { this.predictedFreshWater = v; }

    public Double getPredictedReusedWater() { return predictedReusedWater; }
    public void setPredictedReusedWater(Double v) { this.predictedReusedWater = v; }

    public String getPredictionConfidence() { return predictionConfidence; }
    public void setPredictionConfidence(String v) { this.predictionConfidence = v; }

    // === Inner DTOs ===
    public static class AlertDTO {
        private String date;
        private String alertType;
        private String severity;
        private String message;

        public AlertDTO() {}
        public AlertDTO(String date, String alertType, String severity, String message) {
            this.date = date;
            this.alertType = alertType;
            this.severity = severity;
            this.message = message;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class DailyTrendDTO {
        private String date;
        private double freshWater;
        private double reusedWater;
        private double waterLoss;
        private double reusePercentage;

        public DailyTrendDTO() {}
        public DailyTrendDTO(String date, double freshWater, double reusedWater, double waterLoss, double reusePercentage) {
            this.date = date;
            this.freshWater = freshWater;
            this.reusedWater = reusedWater;
            this.waterLoss = waterLoss;
            this.reusePercentage = reusePercentage;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public double getFreshWater() { return freshWater; }
        public void setFreshWater(double v) { this.freshWater = v; }
        public double getReusedWater() { return reusedWater; }
        public void setReusedWater(double v) { this.reusedWater = v; }
        public double getWaterLoss() { return waterLoss; }
        public void setWaterLoss(double v) { this.waterLoss = v; }
        public double getReusePercentage() { return reusePercentage; }
        public void setReusePercentage(double v) { this.reusePercentage = v; }
    }
}
