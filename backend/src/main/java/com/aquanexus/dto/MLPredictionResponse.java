package com.aquanexus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO returned by Python ML service for next-day predictions.
 */
public class MLPredictionResponse {

    @JsonProperty("predicted_fresh_water")
    private double predictedFreshWater;

    @JsonProperty("predicted_reused_water")
    private double predictedReusedWater;

    @JsonProperty("predicted_reuse_percentage")
    private double predictedReusePercentage;

    @JsonProperty("cto_limit")
    private double ctoLimit;

    @JsonProperty("cto_utilization_predicted")
    private double ctoUtilizationPredicted;

    @JsonProperty("cto_compliance")
    private String ctoCompliance;

    @JsonProperty("model_confidence")
    private String modelConfidence;

    public MLPredictionResponse() {}

    public double getPredictedFreshWater() { return predictedFreshWater; }
    public void setPredictedFreshWater(double v) { this.predictedFreshWater = v; }

    public double getPredictedReusedWater() { return predictedReusedWater; }
    public void setPredictedReusedWater(double v) { this.predictedReusedWater = v; }

    public double getPredictedReusePercentage() { return predictedReusePercentage; }
    public void setPredictedReusePercentage(double v) { this.predictedReusePercentage = v; }

    public double getCtoLimit() { return ctoLimit; }
    public void setCtoLimit(double ctoLimit) { this.ctoLimit = ctoLimit; }

    public double getCtoUtilizationPredicted() { return ctoUtilizationPredicted; }
    public void setCtoUtilizationPredicted(double v) { this.ctoUtilizationPredicted = v; }

    public String getCtoCompliance() { return ctoCompliance; }
    public void setCtoCompliance(String ctoCompliance) { this.ctoCompliance = ctoCompliance; }

    public String getModelConfidence() { return modelConfidence; }
    public void setModelConfidence(String modelConfidence) { this.modelConfidence = modelConfidence; }
}
