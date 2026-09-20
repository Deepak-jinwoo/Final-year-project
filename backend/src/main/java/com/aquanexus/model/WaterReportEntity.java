package com.aquanexus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing generated monthly PDF reports in MySQL.
 */
@Entity
@Table(name = "water_monthly_reports")
public class WaterReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportTitle;          // e.g. "August 2026 Monthly Water Management & CTO Report"

    @Column(nullable = false)
    private String periodName;           // e.g. "August 2026" or "2026-08-01 to 2026-08-31"

    private String industryName;
    private String ctoNumber;

    private LocalDateTime generatedAt;

    private int totalRecordsCount;
    private double totalFreshWater;
    private double totalReusedWater;
    private double totalWaterLoss;
    private double avgReusePercentage;
    private double avgCtoUtilization;
    private int anomalyCount;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] pdfData;              // Binary PDF Content

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String jsonSummary;          // Structured JSON preview summary

    public WaterReportEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }

    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }

    public String getIndustryName() { return industryName; }
    public void setIndustryName(String industryName) { this.industryName = industryName; }

    public String getCtoNumber() { return ctoNumber; }
    public void setCtoNumber(String ctoNumber) { this.ctoNumber = ctoNumber; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public int getTotalRecordsCount() { return totalRecordsCount; }
    public void setTotalRecordsCount(int totalRecordsCount) { this.totalRecordsCount = totalRecordsCount; }

    public double getTotalFreshWater() { return totalFreshWater; }
    public void setTotalFreshWater(double totalFreshWater) { this.totalFreshWater = totalFreshWater; }

    public double getTotalReusedWater() { return totalReusedWater; }
    public void setTotalReusedWater(double totalReusedWater) { this.totalReusedWater = totalReusedWater; }

    public double getTotalWaterLoss() { return totalWaterLoss; }
    public void setTotalWaterLoss(double totalWaterLoss) { this.totalWaterLoss = totalWaterLoss; }

    public double getAvgReusePercentage() { return avgReusePercentage; }
    public void setAvgReusePercentage(double avgReusePercentage) { this.avgReusePercentage = avgReusePercentage; }

    public double getAvgCtoUtilization() { return avgCtoUtilization; }
    public void setAvgCtoUtilization(double avgCtoUtilization) { this.avgCtoUtilization = avgCtoUtilization; }

    public int getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(int anomalyCount) { this.anomalyCount = anomalyCount; }

    public byte[] getPdfData() { return pdfData; }
    public void setPdfData(byte[] pdfData) { this.pdfData = pdfData; }

    public String getJsonSummary() { return jsonSummary; }
    public void setJsonSummary(String jsonSummary) { this.jsonSummary = jsonSummary; }
}
