package com.aquanexus.service;

import com.aquanexus.dto.WaterReuseRecommendationDTO;
import com.aquanexus.dto.WaterReuseRecommendationDTO.ApplicationRecommendationDTO;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.WaterDailyRecordRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service providing rule-based decision support for water reuse applications.
 * Analyzes actual user-submitted water records from MySQL database.
 * Does NOT generate fake data or rely on hardcoded ML outputs.
 */
@Service
public class WaterReuseRecommendationService {

    private final WaterDailyRecordRepository recordRepository;

    public WaterReuseRecommendationService(WaterDailyRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * Compute water reuse recommendations based on actual historical/latest records.
     */
    public WaterReuseRecommendationDTO getRecommendations() {
        WaterReuseRecommendationDTO dto = new WaterReuseRecommendationDTO();

        // Fetch recent records
        List<WaterDailyRecord> records = recordRepository.findAllOrderByDateDesc();

        if (records.isEmpty()) {
            dto.setHasData(false);
            dto.setEmptyStateMessage("No water data available for recommendations. Add daily water usage data to generate reuse recommendations.");
            dto.setAvailableReusableWater(0.0);
            dto.setTotalPotentialFreshSaving(0.0);
            dto.setTopRecommendedReuse("None");
            dto.setQualityVerificationStatus("NOT AVAILABLE");
            dto.setRecommendations(new ArrayList<>());
            return dto;
        }

        // Calculate available reusable water (based on latest daily record or daily average)
        WaterDailyRecord latest = records.get(0);
        double availableReused = latest.getReusedWater() != null ? latest.getReusedWater() : 0.0;
        double freshConsumed = latest.getFreshWaterConsumed() != null ? latest.getFreshWaterConsumed() : 0.0;

        // If latest has 0 reused water, average across all records
        if (availableReused == 0 && records.size() > 1) {
            availableReused = records.stream()
                    .filter(r -> r.getReusedWater() != null)
                    .mapToDouble(WaterDailyRecord::getReusedWater)
                    .average().orElse(0.0);
        }

        availableReused = Math.round(availableReused * 10.0) / 10.0;

        dto.setHasData(true);
        dto.setAvailableReusableWater(availableReused);
        dto.setTotalPotentialFreshSaving(availableReused);
        dto.setQualityVerificationStatus("REQUIRED");

        if (availableReused > 0) {
            dto.setTopRecommendedReuse("Landscaping & Toilet Flushing");
        } else {
            dto.setTopRecommendedReuse("Increase Recycling Efficiency");
        }

        List<ApplicationRecommendationDTO> appList = new ArrayList<>();

        // 1. Landscaping / Gardening
        double landscapeQty = Math.round(Math.min(availableReused * 0.30, availableReused) * 10.0) / 10.0;
        appList.add(new ApplicationRecommendationDTO(
                "Landscaping / Gardening",
                availableReused > 0 ? "RECOMMENDED" : "NOT RECOMMENDED",
                landscapeQty,
                landscapeQty,
                "Suitable for non-potable irrigation when applicable water-quality requirements are satisfied.",
                "Secondary Filtration & UV Disinfection",
                List.of("pH: 6.5 - 8.5", "BOD: < 10 mg/L", "TSS: < 10 mg/L", "Fecal Coliform: < 14/100 mL")
        ));

        // 2. Toilet Flushing
        double toiletQty = Math.round(Math.min(availableReused * 0.35, availableReused) * 10.0) / 10.0;
        appList.add(new ApplicationRecommendationDTO(
                "Toilet Flushing",
                availableReused > 0 ? "CONDITIONAL" : "NOT RECOMMENDED",
                toiletQty,
                toiletQty,
                "Can reduce fresh-water demand for non-potable applications when appropriate treatment is provided.",
                "Dual-stage Filtration & Chlorination",
                List.of("Turbidity: < 2 NTU", "Residual Chlorine: > 1 mg/L", "No odor or color")
        ));

        // 3. Floor / Area Cleaning
        double cleaningQty = Math.round(Math.min(availableReused * 0.20, availableReused) * 10.0) / 10.0;
        appList.add(new ApplicationRecommendationDTO(
                "Floor / Area Cleaning",
                availableReused > 0 ? "CONDITIONAL" : "NOT RECOMMENDED",
                cleaningQty,
                cleaningQty,
                "Applicable for outdoor floor washing and equipment rinse down subject to low turbidity and disinfection checks.",
                "Sand Filtration + Disinfection",
                List.of("TSS: < 5 mg/L", "Oil & Grease: < 1 mg/L")
        ));

        // 4. Cooling Tower Makeup
        double coolingQty = Math.round(Math.min(availableReused * 0.40, availableReused) * 10.0) / 10.0;
        appList.add(new ApplicationRecommendationDTO(
                "Cooling Tower Makeup",
                availableReused > 0 ? "QUALITY VERIFICATION REQUIRED" : "NOT RECOMMENDED",
                coolingQty,
                coolingQty,
                "Water-quality verification required before reuse to verify TDS, hardness, and silica levels to prevent scaling.",
                "Reverse Osmosis (RO) & Biocide Treatment",
                List.of("TDS: < 500 ppm", "Total Hardness: < 50 ppm", "Silica: < 150 ppm")
        ));

        // 5. Suitable Industrial / Process Reuse
        double industrialQty = Math.round(Math.min(availableReused * 0.25, availableReused) * 10.0) / 10.0;
        appList.add(new ApplicationRecommendationDTO(
                "Industrial / Process Reuse",
                availableReused > 0 ? "QUALITY VERIFICATION REQUIRED" : "NOT RECOMMENDED",
                industrialQty,
                industrialQty,
                "Water-quality verification required before reuse. Requires lab verification matching process tolerance standards.",
                "Advanced Oxidation & Ultrafiltration",
                List.of("Process-specific quality standards verification required")
        ));

        dto.setRecommendations(appList);
        return dto;
    }
}
