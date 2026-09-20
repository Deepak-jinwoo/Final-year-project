package com.aquanexus.controller;

import com.aquanexus.dto.WaterRecordInputDTO;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.repository.WaterAlertRepository;
import com.aquanexus.service.WaterCalculationService;
import com.aquanexus.service.WaterReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for water record management, alerts, and reports.
 * All dashboard data is dynamic — computed from actual MySQL records.
 */
@RestController
@RequestMapping("/api/water")
public class WaterRecordController {

    private final WaterCalculationService calculationService;
    private final WaterReportService reportService;
    private final WaterAlertRepository alertRepository;
    private final com.aquanexus.service.WaterReuseRecommendationService recommendationService;
    private final com.aquanexus.service.WaterMonthlyReportService monthlyReportService;
    private final com.aquanexus.service.WaterPDFReportService pdfReportService;

    public WaterRecordController(WaterCalculationService calculationService,
                                  WaterReportService reportService,
                                  WaterAlertRepository alertRepository,
                                  com.aquanexus.service.WaterReuseRecommendationService recommendationService,
                                  com.aquanexus.service.WaterMonthlyReportService monthlyReportService,
                                  com.aquanexus.service.WaterPDFReportService pdfReportService) {
        this.calculationService = calculationService;
        this.reportService = reportService;
        this.alertRepository = alertRepository;
        this.recommendationService = recommendationService;
        this.monthlyReportService = monthlyReportService;
        this.pdfReportService = pdfReportService;
    }

    /**
     * GET /api/water/recommendations
     * Returns rule-based water reuse recommendations based on actual database records.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<com.aquanexus.dto.WaterReuseRecommendationDTO> getRecommendations() {
        return ResponseEntity.ok(recommendationService.getRecommendations());
    }

    /**
     * POST /api/water/records
     * Submit a new daily water usage record.
     * Backend automatically computes reuse%, CTO utilization, water loss,
     * compliance status, anomaly flags, and generates alerts.
     */
    @PostMapping("/records")
    public ResponseEntity<?> submitRecord(@Valid @RequestBody WaterRecordInputDTO input) {
        try {
            WaterDailyRecord saved = calculationService.saveRecord(input);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Record saved. Metrics computed successfully.",
                "record", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/water/records?startDate=...&endDate=...&department=...
     * Fetch historical records with optional filters.
     */
    @GetMapping("/records")
    public ResponseEntity<List<WaterDailyRecord>> getRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(calculationService.getRecords(startDate, endDate, department));
    }

    /**
     * GET /api/water/dashboard-summary?days=30
     * Returns fully dynamic dashboard data computed from MySQL.
     */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<?> getDashboardSummary(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(calculationService.getDashboardSummary(days));
    }

    /**
     * GET /api/water/alerts
     * Returns active (unresolved) alerts.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<WaterAlert>> getAlerts() {
        return ResponseEntity.ok(alertRepository.findByIsResolvedFalseOrderByCreatedAtDesc());
    }

    /**
     * GET /api/water/reports?startDate=...&endDate=...
     * Returns structured JSON report.
     */
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(reportService.generateReport(startDate, endDate));
    }

    /**
     * GET /api/water/reports/export/csv?startDate=...&endDate=...
     * Exports records as downloadable CSV file.
     */
    @GetMapping("/reports/export/csv")
    public ResponseEntity<String> exportCSV(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        String csv = reportService.generateCSV(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=water_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    /**
     * DELETE /api/water/records/{id}
     * Delete a water record by ID.
     */
    @DeleteMapping("/records/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id) {
        boolean deleted = calculationService.deleteRecord(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Record deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/water/reports/history
     * List all generated monthly reports metadata.
     */
    @GetMapping("/reports/history")
    public ResponseEntity<?> getReportHistory() {
        return ResponseEntity.ok(monthlyReportService.getAllReportHistory());
    }

    /**
     * POST /api/water/reports/generate
     * Generate monthly PDF report on demand.
     */
    @PostMapping("/reports/generate")
    public ResponseEntity<?> generateMonthlyReport(@RequestBody(required = false) Map<String, String> body) {
        String startDateStr = body != null ? body.get("startDate") : null;
        String endDateStr = body != null ? body.get("endDate") : null;
        String customPeriod = body != null ? body.get("periodName") : null;

        java.time.LocalDate start = startDateStr != null ? java.time.LocalDate.parse(startDateStr) : java.time.LocalDate.now().withDayOfMonth(1);
        java.time.LocalDate end = endDateStr != null ? java.time.LocalDate.parse(endDateStr) : java.time.LocalDate.now();

        com.aquanexus.model.WaterReportEntity report = monthlyReportService.generateMonthlyReport(start, end, customPeriod);
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/water/reports/{id}/pdf
     * Download binary PDF report file.
     */
    @GetMapping("/reports/{id}/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Long id) {
        var opt = monthlyReportService.getReportById(id);
        if (opt.isPresent() && opt.get().getPdfData() != null) {
            var r = opt.get();
            String pName = r.getPeriodName() != null ? r.getPeriodName().replaceAll("[^a-zA-Z0-9]", "_") : "August_2026";
            String filename = "AquaNexus_Monthly_Water_Report_" + pName + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(r.getPdfData());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/water/reports/{id}/preview
     * Get JSON preview for report modal.
     */
    @GetMapping("/reports/{id}/preview")
    public ResponseEntity<?> getReportPreview(@PathVariable Long id) {
        var opt = monthlyReportService.getReportById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(opt.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/water/company-profile
     * Fetch saved company/facility profile for auto-fill in daily data entry modal.
     */
    @GetMapping("/company-profile")
    public ResponseEntity<Map<String, String>> getCompanyProfile() {
        return ResponseEntity.ok(calculationService.getLatestCompanyProfile());
    }

    /**
     * GET /api/water/reports/daily/{recordId}/pdf
     * Download daily PDF report for a specific record.
     */
    @GetMapping("/reports/daily/{recordId}/pdf")
    public ResponseEntity<byte[]> downloadDailyReportPdf(@PathVariable Long recordId) {
        var recOpt = calculationService.getLatestCompanyProfile(); // fallback
        byte[] pdfBytes = monthlyReportService.generateDailyReportPdf(recordId);
        var preview = monthlyReportService.getDailyReportPreview(recordId);
        String dateStr = preview.get("date") != null ? preview.get("date").toString() : "2026-08-15";
        String filename = "AquaNexus_Daily_Water_Report_" + dateStr + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * GET /api/water/reports/daily/{recordId}/preview
     * Get preview summary JSON for daily report modal viewer.
     */
    @GetMapping("/reports/daily/{recordId}/preview")
    public ResponseEntity<?> getDailyReportPreview(@PathVariable Long recordId) {
        return ResponseEntity.ok(monthlyReportService.getDailyReportPreview(recordId));
    }

    /**
     * GET /api/water/reports/export/pdf
     * Generate a professional A4 industrial water-consumption report PDF.
     * Accepts query params: startDate, endDate, department (optional), industryId (optional), preparedBy (optional).
     */
    @GetMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportConsumptionReportPdf(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer industryId,
            @RequestParam(required = false) String preparedBy) {

        byte[] pdfBytes = pdfReportService.generateWaterConsumptionReport(industryId, startDate, endDate, department, preparedBy);
        String filename = "AquaNexus_Water_Consumption_Report_" + startDate + "_to_" + endDate + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * POST /api/water/reset-all-data
     * Clears all legacy records, alerts, and notification logs for a pure clean state.
     */
    @PostMapping("/reset-all-data")
    public ResponseEntity<Map<String, Object>> resetAllData() {
        calculationService.resetAllData();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "All water records, alerts, and logs have been cleared. System is in pure zero-record state.");
        return ResponseEntity.ok(resp);
    }
}


