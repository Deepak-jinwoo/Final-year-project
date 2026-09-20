package com.aquanexus.dto;

import java.util.List;

/**
 * DTO for Water Reuse Recommendations module.
 * Summarizes reusable water availability, potential fresh water savings,
 * and application-specific recommendation cards.
 */
public class WaterReuseRecommendationDTO {

    private double availableReusableWater;      // Total treated/reusable water (L/day)
    private double totalPotentialFreshSaving;   // Total potential fresh water saving (L/day)
    private String topRecommendedReuse;        // e.g. "Landscaping & Toilet Flushing"
    private String qualityVerificationStatus;   // "REQUIRED", "VERIFIED", "NOT AVAILABLE"
    private boolean hasData;                    // true if user records exist
    private String emptyStateMessage;

    private List<ApplicationRecommendationDTO> recommendations;

    public WaterReuseRecommendationDTO() {}

    // Getters & Setters
    public double getAvailableReusableWater() { return availableReusableWater; }
    public void setAvailableReusableWater(double v) { this.availableReusableWater = v; }

    public double getTotalPotentialFreshSaving() { return totalPotentialFreshSaving; }
    public void setTotalPotentialFreshSaving(double v) { this.totalPotentialFreshSaving = v; }

    public String getTopRecommendedReuse() { return topRecommendedReuse; }
    public void setTopRecommendedReuse(String v) { this.topRecommendedReuse = v; }

    public String getQualityVerificationStatus() { return qualityVerificationStatus; }
    public void setQualityVerificationStatus(String v) { this.qualityVerificationStatus = v; }

    public boolean isHasData() { return hasData; }
    public void setHasData(boolean v) { this.hasData = v; }

    public String getEmptyStateMessage() { return emptyStateMessage; }
    public void setEmptyStateMessage(String v) { this.emptyStateMessage = v; }

    public List<ApplicationRecommendationDTO> getRecommendations() { return recommendations; }
    public void setRecommendations(List<ApplicationRecommendationDTO> v) { this.recommendations = v; }

    // Inner DTO for individual application recommendations
    public static class ApplicationRecommendationDTO {
        private String applicationName;         // e.g., "Landscaping / Gardening"
        private String status;                  // "RECOMMENDED", "CONDITIONAL", "QUALITY VERIFICATION REQUIRED", "NOT RECOMMENDED"
        private double potentialReuseQuantity;  // (L/day)
        private double potentialFreshSaving;    // (L/day)
        private String reason;
        private String requiredTreatment;
        private List<String> qualityRequirements;

        public ApplicationRecommendationDTO() {}

        public ApplicationRecommendationDTO(String name, String status, double reuseQty, double saving, String reason, String treatment, List<String> qualityRequirements) {
            this.applicationName = name;
            this.status = status;
            this.potentialReuseQuantity = reuseQty;
            this.potentialFreshSaving = saving;
            this.reason = reason;
            this.requiredTreatment = treatment;
            this.qualityRequirements = qualityRequirements;
        }

        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String v) { this.applicationName = v; }

        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }

        public double getPotentialReuseQuantity() { return potentialReuseQuantity; }
        public void setPotentialReuseQuantity(double v) { this.potentialReuseQuantity = v; }

        public double getPotentialFreshSaving() { return potentialFreshSaving; }
        public void setPotentialFreshSaving(double v) { this.potentialFreshSaving = v; }

        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }

        public String getRequiredTreatment() { return requiredTreatment; }
        public void setRequiredTreatment(String v) { this.requiredTreatment = v; }

        public List<String> getQualityRequirements() { return qualityRequirements; }
        public void setQualityRequirements(List<String> v) { this.qualityRequirements = v; }
    }
}
