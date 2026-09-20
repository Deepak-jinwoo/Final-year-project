package com.aquanexus.service;

import com.aquanexus.dto.*;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.WaterDailyRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer acting as an HTTP client to the Python FastAPI ML microservice.
 * Demonstrates a modular Java architecture where Spring Boot coordinates data
 * fetching and delegates ML inference to Python.
 */
@Service
public class MLClientService {

    private final WaterDailyRecordRepository recordRepository;
    private final RestClient restClient;

    public MLClientService(WaterDailyRecordRepository recordRepository,
                           @Value("${app.ml-service.base-url:http://localhost:5000}") String mlBaseUrl) {
        this.recordRepository = recordRepository;
        this.restClient = RestClient.builder()
                .baseUrl(mlBaseUrl)
                .build();
    }

    /**
     * Get next-day fresh water & reused water predictions for a given industry.
     */
    public MLPredictionResponse getPredictionsForIndustry(Integer industryId) {
        List<WaterDailyRecord> records = recordRepository.findTop14ByIndustryIdOrderByDateDesc(industryId);
        
        if (records.isEmpty()) {
            throw new RuntimeException("No water usage records found for industry ID: " + industryId);
        }

        List<MLPredictionRequest.MLDailyRecordDTO> dtoList = mapToDTOList(records);
        MLPredictionRequest requestPayload = new MLPredictionRequest(dtoList);

        return restClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(MLPredictionResponse.class);
    }

    /**
     * Detect anomalies (leaks/inefficiencies) in recent records for a given industry.
     */
    public MLAnomalyResponse detectAnomaliesForIndustry(Integer industryId) {
        List<WaterDailyRecord> records = recordRepository.findByIndustryIdOrderByDateAsc(industryId);

        if (records.isEmpty()) {
            throw new RuntimeException("No water usage records found for industry ID: " + industryId);
        }

        List<MLPredictionRequest.MLDailyRecordDTO> dtoList = mapToDTOList(records);
        MLPredictionRequest requestPayload = new MLPredictionRequest(dtoList);

        return restClient.post()
                .uri("/detect-anomalies")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(MLAnomalyResponse.class);
    }

    private List<MLPredictionRequest.MLDailyRecordDTO> mapToDTOList(List<WaterDailyRecord> records) {
        return records.stream().map(r -> {
            MLPredictionRequest.MLDailyRecordDTO dto = new MLPredictionRequest.MLDailyRecordDTO();
            dto.setIndustry_id(r.getIndustryId());
            dto.setDepartment(r.getDepartment() != null ? r.getDepartment() : "General");
            dto.setDate(r.getDate().toString());
            dto.setFresh_water_consumed(r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0.0);
            dto.setWastewater_generated(r.getWastewaterGenerated() != null ? r.getWastewaterGenerated() : 0.0);
            dto.setReused_water(r.getReusedWater() != null ? r.getReusedWater() : 0.0);
            dto.setCto_limit(r.getCtoLimit() != null ? r.getCtoLimit() : 2000.0);
            return dto;
        }).collect(Collectors.toList());
    }
}
