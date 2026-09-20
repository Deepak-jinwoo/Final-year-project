package com.aquanexus.controller;

import com.aquanexus.dto.MLAnomalyResponse;
import com.aquanexus.dto.MLPredictionResponse;
import com.aquanexus.service.MLClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller exposing ML prediction & anomaly detection endpoints to the frontend.
 * Pure Spring Boot controller layer delegating ML operations to MLClientService.
 */
@RestController
@RequestMapping("/api/ml")
public class MLController {

    private final MLClientService mlClientService;

    public MLController(MLClientService mlClientService) {
        this.mlClientService = mlClientService;
    }

    /**
     * GET /api/ml/predict/{industryId}
     * Predicts next-day fresh water & reused water consumption.
     */
    @GetMapping("/predict/{industryId}")
    public ResponseEntity<?> predictNextDay(@PathVariable Integer industryId) {
        try {
            MLPredictionResponse response = mlClientService.getPredictionsForIndustry(industryId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * GET /api/ml/anomalies/{industryId}
     * Performs anomaly detection (leak/inefficiency detection) on daily usage records.
     */
    @GetMapping("/anomalies/{industryId}")
    public ResponseEntity<?> detectAnomalies(@PathVariable Integer industryId) {
        try {
            MLAnomalyResponse response = mlClientService.detectAnomaliesForIndustry(industryId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }
}
