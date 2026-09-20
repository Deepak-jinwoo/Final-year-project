package com.aquanexus.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for daily water usage records.
 * Stores raw user inputs AND pre-computed metrics (reuse %, CTO utilization, etc.).
 */
@Entity
@Table(name = "water_daily_records")
public class WaterDailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "industry_id", nullable = false)
    private Integer industryId;

    @Column(name = "industry_name", length = 200)
    private String industryName;

    @Column(name = "cto_number", length = 100)
    private String ctoNumber;

    @Column(name = "facility_location", length = 255)
    private String facilityLocation;

    @Column(name = "plant_id", length = 100)
    private String plantId;

    @Column(name = "industry_type", length = 100)
    private String industryType;

    @Column(name = "responsible_officer", length = 100)
    private String responsibleOfficer;

    @Column(name = "contact_info", length = 200)
    private String contactInfo;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // === Raw User Inputs ===
    @Column(name = "fresh_water_consumed")
    private Double freshWaterConsumed;

    @Column(name = "wastewater_generated")
    private Double wastewaterGenerated;

    @Column(name = "reused_water")
    private Double reusedWater;

    @Column(name = "cto_limit")
    private Double ctoLimit;

    // === Computed Metrics (calculated by WaterCalculationService) ===
    @Column(name = "reuse_percentage")
    private Double reusePercentage;

    @Column(name = "cto_utilization")
    private Double ctoUtilization;

    @Column(name = "water_loss")
    private Double waterLoss;

    @Column(name = "compliance_status", length = 20)
    private String complianceStatus;  // SAFE, WARNING, CRITICAL

    @Column(name = "is_anomaly")
    private Boolean isAnomaly = false;

    @Column(name = "treated_water_available")
    private Double treatedWaterAvailable;

    @Column(name = "main_meter_consumption")
    private Double mainMeterConsumption;

    @Column(name = "total_department_consumption")
    private Double totalDepartmentConsumption;

    @Column(name = "opening_meter_reading")
    private Double openingMeterReading;

    @Column(name = "closing_meter_reading")
    private Double closingMeterReading;

    @Column(name = "unaccounted_water")
    private Double unaccountedWater;

    @Column(name = "reuse_recovery_rate")
    private Double reuseRecoveryRate;

    @Column(name = "meter_record_type", length = 100)
    private String meterRecordType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // === Constructors ===
    public WaterDailyRecord() {}

    // === Getters & Setters ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIndustryId() { return industryId; }
    public void setIndustryId(Integer industryId) { this.industryId = industryId; }

    public String getIndustryName() { return industryName; }
    public void setIndustryName(String industryName) { this.industryName = industryName; }

    public String getCtoNumber() { return ctoNumber; }
    public void setCtoNumber(String ctoNumber) { this.ctoNumber = ctoNumber; }

    public String getFacilityLocation() { return facilityLocation; }
    public void setFacilityLocation(String facilityLocation) { this.facilityLocation = facilityLocation; }

    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }

    public String getIndustryType() { return industryType; }
    public void setIndustryType(String industryType) { this.industryType = industryType; }

    public String getResponsibleOfficer() { return responsibleOfficer; }
    public void setResponsibleOfficer(String responsibleOfficer) { this.responsibleOfficer = responsibleOfficer; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Double getFreshWaterConsumed() { return freshWaterConsumed; }
    public void setFreshWaterConsumed(Double freshWaterConsumed) { this.freshWaterConsumed = freshWaterConsumed; }

    public Double getWastewaterGenerated() { return wastewaterGenerated; }
    public void setWastewaterGenerated(Double wastewaterGenerated) { this.wastewaterGenerated = wastewaterGenerated; }

    public Double getReusedWater() { return reusedWater; }
    public void setReusedWater(Double reusedWater) { this.reusedWater = reusedWater; }

    public Double getCtoLimit() { return ctoLimit; }
    public void setCtoLimit(Double ctoLimit) { this.ctoLimit = ctoLimit; }

    public Double getReusePercentage() { return reusePercentage; }
    public void setReusePercentage(Double reusePercentage) { this.reusePercentage = reusePercentage; }

    public Double getCtoUtilization() { return ctoUtilization; }
    public void setCtoUtilization(Double ctoUtilization) { this.ctoUtilization = ctoUtilization; }

    public Double getWaterLoss() { return waterLoss; }
    public void setWaterLoss(Double waterLoss) { this.waterLoss = waterLoss; }

    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

    public Boolean getIsAnomaly() { return isAnomaly; }
    public void setIsAnomaly(Boolean isAnomaly) { this.isAnomaly = isAnomaly; }

    public Double getTreatedWaterAvailable() { return treatedWaterAvailable; }
    public void setTreatedWaterAvailable(Double treatedWaterAvailable) { this.treatedWaterAvailable = treatedWaterAvailable; }

    public Double getMainMeterConsumption() { return mainMeterConsumption; }
    public void setMainMeterConsumption(Double mainMeterConsumption) { this.mainMeterConsumption = mainMeterConsumption; }

    public Double getTotalDepartmentConsumption() { return totalDepartmentConsumption; }
    public void setTotalDepartmentConsumption(Double totalDepartmentConsumption) { this.totalDepartmentConsumption = totalDepartmentConsumption; }

    public Double getOpeningMeterReading() { return openingMeterReading; }
    public void setOpeningMeterReading(Double openingMeterReading) { this.openingMeterReading = openingMeterReading; }

    public Double getClosingMeterReading() { return closingMeterReading; }
    public void setClosingMeterReading(Double closingMeterReading) { this.closingMeterReading = closingMeterReading; }

    public Double getUnaccountedWater() { return unaccountedWater; }
    public void setUnaccountedWater(Double unaccountedWater) { this.unaccountedWater = unaccountedWater; }

    public Double getReuseRecoveryRate() { return reuseRecoveryRate; }
    public void setReuseRecoveryRate(Double reuseRecoveryRate) { this.reuseRecoveryRate = reuseRecoveryRate; }

    public String getMeterRecordType() { return meterRecordType; }
    public void setMeterRecordType(String meterRecordType) { this.meterRecordType = meterRecordType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
