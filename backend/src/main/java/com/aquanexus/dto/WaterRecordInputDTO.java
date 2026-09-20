package com.aquanexus.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for user-submitted daily water record input.
 * All raw values — metrics are computed by the backend.
 */
public class WaterRecordInputDTO {

    @NotNull(message = "Industry ID is required")
    private Integer industryId;

    private String industryName;
    private String ctoNumber;
    private String facilityLocation;
    private String plantId;
    private String industryType;
    private String responsibleOfficer;
    private String contactInfo;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Date is required")
    private String date;  // "YYYY-MM-DD"

    @NotNull(message = "Fresh water consumed is required")
    @Min(value = 0, message = "Fresh water consumed must be >= 0")
    private Double freshWaterConsumed;

    @NotNull(message = "Wastewater generated is required")
    @Min(value = 0, message = "Wastewater generated must be >= 0")
    private Double wastewaterGenerated;

    @NotNull(message = "Reused water is required")
    @Min(value = 0, message = "Reused water must be >= 0")
    private Double reusedWater;

    @NotNull(message = "CTO limit is required")
    @Min(value = 1, message = "CTO limit must be > 0")
    private Double ctoLimit;

    private Double treatedWaterAvailable;
    private Double mainMeterConsumption;
    private Double totalDepartmentConsumption;
    private Double openingMeterReading;
    private Double closingMeterReading;
    private String meterRecordType;

    // Getters & Setters
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

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Double getFreshWaterConsumed() { return freshWaterConsumed; }
    public void setFreshWaterConsumed(Double freshWaterConsumed) { this.freshWaterConsumed = freshWaterConsumed; }

    public Double getWastewaterGenerated() { return wastewaterGenerated; }
    public void setWastewaterGenerated(Double wastewaterGenerated) { this.wastewaterGenerated = wastewaterGenerated; }

    public Double getReusedWater() { return reusedWater; }
    public void setReusedWater(Double reusedWater) { this.reusedWater = reusedWater; }

    public Double getCtoLimit() { return ctoLimit; }
    public void setCtoLimit(Double ctoLimit) { this.ctoLimit = ctoLimit; }

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

    public String getMeterRecordType() { return meterRecordType; }
    public void setMeterRecordType(String meterRecordType) { this.meterRecordType = meterRecordType; }
}
