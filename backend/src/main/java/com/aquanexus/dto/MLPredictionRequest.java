package com.aquanexus.dto;

import java.util.List;

/**
 * Request DTO sent to the Python ML service for prediction.
 */
public class MLPredictionRequest {

    private List<MLDailyRecordDTO> records;

    public MLPredictionRequest() {}

    public MLPredictionRequest(List<MLDailyRecordDTO> records) {
        this.records = records;
    }

    public List<MLDailyRecordDTO> getRecords() { return records; }
    public void setRecords(List<MLDailyRecordDTO> records) { this.records = records; }

    /**
     * Inner DTO representing a single daily record sent to the ML service.
     * Uses snake_case field names to match the Python FastAPI model.
     */
    public static class MLDailyRecordDTO {
        private int industry_id;
        private String department;
        private String date;
        private double fresh_water_consumed;
        private double wastewater_generated;
        private double reused_water;
        private double cto_limit;

        public MLDailyRecordDTO() {}

        public int getIndustry_id() { return industry_id; }
        public void setIndustry_id(int industry_id) { this.industry_id = industry_id; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public double getFresh_water_consumed() { return fresh_water_consumed; }
        public void setFresh_water_consumed(double v) { this.fresh_water_consumed = v; }

        public double getWastewater_generated() { return wastewater_generated; }
        public void setWastewater_generated(double v) { this.wastewater_generated = v; }

        public double getReused_water() { return reused_water; }
        public void setReused_water(double v) { this.reused_water = v; }

        public double getCto_limit() { return cto_limit; }
        public void setCto_limit(double v) { this.cto_limit = v; }
    }
}
