package com.aquanexus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO returned by Python ML service for anomaly detection.
 */
public class MLAnomalyResponse {

    @JsonProperty("total_records")
    private int totalRecords;

    @JsonProperty("anomalies_detected")
    private int anomaliesDetected;

    @JsonProperty("results")
    private List<AnomalyRecordDTO> results;

    public MLAnomalyResponse() {}

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getAnomaliesDetected() { return anomaliesDetected; }
    public void setAnomaliesDetected(int anomaliesDetected) { this.anomaliesDetected = anomaliesDetected; }

    public List<AnomalyRecordDTO> getResults() { return results; }
    public void setResults(List<AnomalyRecordDTO> results) { this.results = results; }

    public static class AnomalyRecordDTO {
        private String date;

        @JsonProperty("fresh_water_consumed")
        private double freshWaterConsumed;

        @JsonProperty("wastewater_generated")
        private double wastewaterGenerated;

        @JsonProperty("reused_water")
        private double reusedWater;

        @JsonProperty("is_anomaly")
        private boolean isAnomaly;

        @JsonProperty("anomaly_score")
        private double anomalyScore;

        private String severity;
        private String reason;

        public AnomalyRecordDTO() {}

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public double getFreshWaterConsumed() { return freshWaterConsumed; }
        public void setFreshWaterConsumed(double v) { this.freshWaterConsumed = v; }

        public double getWastewaterGenerated() { return wastewaterGenerated; }
        public void setWastewaterGenerated(double v) { this.wastewaterGenerated = v; }

        public double getReusedWater() { return reusedWater; }
        public void setReusedWater(double v) { this.reusedWater = v; }

        public boolean isAnomaly() { return isAnomaly; }
        public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }

        public double getAnomalyScore() { return anomalyScore; }
        public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
