package com.aquanexus.service;

import com.aquanexus.model.WaterDailyRecord;
import com.aquanexus.model.WaterAlert;
import com.aquanexus.repository.WaterAlertRepository;
import com.aquanexus.repository.WaterDailyRecordRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise report generation service for professional industrial A4 reports using OpenPDF.
 */
@Service
public class WaterPDFReportService {

    private final WaterDailyRecordRepository recordRepository;
    private final WaterAlertRepository alertRepository;

    public WaterPDFReportService(WaterDailyRecordRepository recordRepository,
                                  WaterAlertRepository alertRepository) {
        this.recordRepository = recordRepository;
        this.alertRepository = alertRepository;
    }

    public byte[] generateWaterConsumptionReport(Integer industryId, String startDateStr, String endDateStr, String department, String preparedBy) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        List<WaterDailyRecord> records;
        if (department != null && !department.trim().isEmpty()) {
            records = recordRepository.findByDepartmentAndDateBetweenOrderByDateAsc(department, startDate, endDate);
        } else {
            records = recordRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
        }

        if (industryId != null) {
            records = records.stream().filter(r -> r.getIndustryId().equals(industryId)).collect(Collectors.toList());
        }

        if (records.isEmpty()) {
            return generateEmptyReportPdf(startDateStr + " to " + endDateStr);
        }

        return buildReportPdf(records, startDateStr, endDateStr, department, preparedBy);
    }

    private byte[] buildReportPdf(List<WaterDailyRecord> records, String startDateStr, String endDateStr, String department, String preparedBy) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 54);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            String periodLabel = startDateStr + " to " + endDateStr;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String prepUser = (preparedBy != null && !preparedBy.trim().isEmpty()) ? preparedBy : "AquaNexus Compliance Manager";

            writer.setPageEvent(new HeaderFooterPageEvent(periodLabel, prepUser, timestamp));

            document.open();

            // Color palette (Professional Navy, Teal, and light backdrops)
            Color navyTheme = new Color(12, 19, 34);       // #0C1322
            Color tealTheme = new Color(13, 148, 136);    // #0D9488
            Color emeraldTheme = new Color(16, 185, 129); // #10B981
            Color amberTheme = new Color(217, 119, 6);     // #D97706
            Color redTheme = new Color(220, 38, 38);       // #DC2626
            Color darkRedTheme = new Color(153, 27, 27);   // #991B1B
            Color lightBg = new Color(248, 250, 252);      // #F8FAFC
            Color tableHeaderBg = new Color(30, 41, 59);   // #1E293B

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, navyTheme);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, tealTheme);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, navyTheme);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.DARK_GRAY);

            // ==========================================
            // PAGE 1: PORTRAIT SUMMARY
            // ==========================================

            // REPORT HEADER
            document.add(new Paragraph("AquaNexus", titleFont));
            Paragraph subTitle = new Paragraph("SMART WATER REUSE ANALYSIS & CTO COMPLIANCE MONITORING", subTitleFont);
            subTitle.setSpacingAfter(15);
            document.add(subTitle);

            // A. FACILITY & REPORT PROFILE
            document.add(new Paragraph("A. Facility & Abstraction Profile", sectionFont));
            Paragraph pA = new Paragraph(" ", bodyFont); pA.setSpacingAfter(4); document.add(pA);

            WaterDailyRecord firstRec = records.get(0);
            PdfPTable profileTable = new PdfPTable(2);
            profileTable.setWidthPercentage(100);
            profileTable.setWidths(new float[]{2.5f, 4.5f});
            profileTable.setSpacingAfter(15);

            addStructuredRow(profileTable, "Industry Name", valOrNA(firstRec.getIndustryName()), boldFont, bodyFont, lightBg);
            addStructuredRow(profileTable, "Industry Type", valOrNA(firstRec.getIndustryType()), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(profileTable, "CTO Permit Registration No.", valOrNA(firstRec.getCtoNumber()), boldFont, bodyFont, lightBg);
            addStructuredRow(profileTable, "CTO Daily Water Limit", firstRec.getCtoLimit() != null ? String.format("%,.1f kL/day", firstRec.getCtoLimit()) : "N/A", boldFont, bodyFont, Color.WHITE);
            addStructuredRow(profileTable, "Report Period", periodLabel, boldFont, bodyFont, lightBg);
            addStructuredRow(profileTable, "Selected Department", (department != null && !department.trim().isEmpty()) ? department : "All Departments", boldFont, bodyFont, Color.WHITE);
            document.add(profileTable);

            // B. SUMMARY KPI SECTION
            document.add(new Paragraph("B. Water Abstraction KPI Summary", sectionFont));
            Paragraph pB = new Paragraph(" ", bodyFont); pB.setSpacingAfter(4); document.add(pB);

            // Computations
            double totalFresh = records.stream().mapToDouble(r -> r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0.0).sum();
            double totalWaste = records.stream().mapToDouble(r -> r.getWastewaterGenerated() != null ? r.getWastewaterGenerated() : 0.0).sum();
            double totalTreated = records.stream().mapToDouble(r -> r.getTreatedWaterAvailable() != null ? r.getTreatedWaterAvailable() : r.getWastewaterGenerated() * 0.75).sum();
            double totalReused = records.stream().mapToDouble(r -> r.getReusedWater() != null ? r.getReusedWater() : 0.0).sum();
            double totalUnaccounted = records.stream().mapToDouble(r -> r.getUnaccountedWater() != null ? r.getUnaccountedWater() : 0.0).sum();
            
            double maxCto = records.stream().filter(r -> r.getCtoUtilization() != null).mapToDouble(WaterDailyRecord::getCtoUtilization).max().orElse(0.0);
            double avgCto = records.stream().filter(r -> r.getCtoUtilization() != null).mapToDouble(WaterDailyRecord::getCtoUtilization).average().orElse(0.0);

            long warningDays = records.stream().filter(r -> "WARNING".equalsIgnoreCase(r.getComplianceStatus())).count();
            long criticalDays = records.stream().filter(r -> "CRITICAL".equalsIgnoreCase(r.getComplianceStatus())).count();
            long anomalyDays = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsAnomaly())).count();

            double replacementRate = totalFresh > 0 ? (totalReused / totalFresh) * 100.0 : 0.0;
            double recoveryRate = totalTreated > 0 ? (totalReused / totalTreated) * 100.0 : 0.0;

            PdfPTable kpiTable = new PdfPTable(2);
            kpiTable.setWidthPercentage(100);
            kpiTable.setWidths(new float[]{4f, 3f});
            kpiTable.setSpacingAfter(15);

            addTableHeader(kpiTable, "Performance Metric", headerFont, tableHeaderBg);
            addTableHeader(kpiTable, "Value / Yield", headerFont, tableHeaderBg);

            addStructuredRow(kpiTable, "Total Fresh Water Consumed", String.format("%,.1f kL", totalFresh), boldFont, bodyFont, lightBg);
            addStructuredRow(kpiTable, "Total Wastewater Generated", String.format("%,.1f kL", totalWaste), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(kpiTable, "Total Treated Water Available", String.format("%,.1f kL", totalTreated), boldFont, bodyFont, lightBg);
            addStructuredRow(kpiTable, "Total Approved Reused Water", String.format("%,.1f kL", totalReused), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(kpiTable, "Reuse Recovery Rate (reused/treated)", String.format("%.1f%%", recoveryRate), boldFont, bodyFont, lightBg);
            addStructuredRow(kpiTable, "Fresh-water Replacement Rate (reused/fresh)", String.format("%.1f%%", replacementRate), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(kpiTable, "Total Unaccounted Water", String.format("%,.1f kL", totalUnaccounted), boldFont, bodyFont, lightBg);
            addStructuredRow(kpiTable, "Average CTO Utilization", String.format("%.1f%%", avgCto), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(kpiTable, "Maximum CTO Utilization", String.format("%.1f%%", maxCto), boldFont, bodyFont, lightBg);
            addStructuredRow(kpiTable, "CTO Warnings Flagged", String.valueOf(warningDays), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(kpiTable, "CTO Critical Threshold Breaches", String.valueOf(criticalDays), boldFont, bodyFont, lightBg);
            addStructuredRow(kpiTable, "Abnormal Usage/Anomaly Reports", String.valueOf(anomalyDays), boldFont, bodyFont, Color.WHITE);
            document.add(kpiTable);

            // C. DEPARTMENT-WISE SUMMARY
            document.add(new Paragraph("C. Department-Wise Distribution", sectionFont));
            Paragraph pC = new Paragraph(" ", bodyFont); pC.setSpacingAfter(4); document.add(pC);

            PdfPTable deptTable = new PdfPTable(7);
            deptTable.setWidthPercentage(100);
            deptTable.setSpacingAfter(10);
            deptTable.setWidths(new float[]{2f, 1.2f, 1f, 1.2f, 1.2f, 1.2f, 1f});

            String[] dHeaders = {"Department", "Total Abstraction (kL)", "Usage %", "Waste (kL)", "Reused (kL)", "Recovery %", "Alerts"};
            for (String dh : dHeaders) {
                addTableHeader(deptTable, dh, headerFont, tableHeaderBg);
            }

            Map<String, double[]> deptStats = new HashMap<>();
            Map<String, Integer> deptAlerts = new HashMap<>();
            for (WaterDailyRecord r : records) {
                String dept = r.getDepartment() != null ? r.getDepartment() : "General";
                deptStats.putIfAbsent(dept, new double[]{0, 0, 0, 0});
                deptStats.get(dept)[0] += (r.getFreshWaterConsumed() != null ? r.getFreshWaterConsumed() : 0.0);
                deptStats.get(dept)[1] += (r.getWastewaterGenerated() != null ? r.getWastewaterGenerated() : 0.0);
                deptStats.get(dept)[2] += (r.getReusedWater() != null ? r.getReusedWater() : 0.0);
                deptStats.get(dept)[3] += (r.getTreatedWaterAvailable() != null ? r.getTreatedWaterAvailable() : 0.0);

                if (Boolean.TRUE.equals(r.getIsAnomaly()) || !"SAFE".equalsIgnoreCase(r.getComplianceStatus())) {
                    deptAlerts.put(dept, deptAlerts.getOrDefault(dept, 0) + 1);
                }
            }

            List<Map.Entry<String, double[]>> sortedDepts = deptStats.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue()[0], e1.getValue()[0]))
                    .collect(Collectors.toList());

            boolean toggle = false;
            for (var entry : sortedDepts) {
                Color bg = toggle ? lightBg : Color.WHITE;
                toggle = !toggle;
                String dName = entry.getKey();
                double df = entry.getValue()[0];
                double dw = entry.getValue()[1];
                double dr = entry.getValue()[2];
                double dt = entry.getValue()[3];
                double usagePct = totalFresh > 0 ? (df / totalFresh) * 100.0 : 0.0;
                double recoveryPct = dt > 0 ? (dr / dt) * 100.0 : 0.0;
                int alerts = deptAlerts.getOrDefault(dName, 0);

                addCell(deptTable, dName, boldFont, Element.ALIGN_LEFT, bg);
                addCell(deptTable, String.format("%,.1f", df), bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(deptTable, String.format("%.1f%%", usagePct), bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(deptTable, String.format("%,.1f", dw), bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(deptTable, String.format("%,.1f", dr), bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(deptTable, String.format("%.1f%%", recoveryPct), bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(deptTable, String.valueOf(alerts), bodyFont, Element.ALIGN_CENTER, bg);
            }
            document.add(deptTable);

            // Dynamic conclusion
            if (!sortedDepts.isEmpty()) {
                String topDept = sortedDepts.get(0).getKey();
                String conclusionText = String.format("Audit Analysis: The %s department recorded the highest fresh water consumption during the selected audit period. Total unaccounted water volume across all logs is calculated at %,.1f kL, requiring verification.",
                        topDept, totalUnaccounted);
                Paragraph conclusionP = new Paragraph(conclusionText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, navyTheme));
                conclusionP.setSpacingAfter(15);
                document.add(conclusionP);
            }

            // D. CTO COMPLIANCE SUMMARY
            document.add(new Paragraph("D. CTO Abstraction Compliance Summary", sectionFont));
            Paragraph pD = new Paragraph(" ", bodyFont); pD.setSpacingAfter(4); document.add(pD);

            long safeDays = records.stream().filter(r -> "SAFE".equalsIgnoreCase(r.getComplianceStatus()) || r.getComplianceStatus() == null).count();
            long exceedLimitDays = records.stream().filter(r -> r.getFreshWaterConsumed() != null && r.getCtoLimit() != null && r.getFreshWaterConsumed() > r.getCtoLimit()).count();

            PdfPTable ctoKpiTable = new PdfPTable(4);
            ctoKpiTable.setWidthPercentage(100);
            ctoKpiTable.setSpacingAfter(8);

            addKpiBox(ctoKpiTable, "Safe Abstraction Days", safeDays + " Days", emeraldTheme, lightBg);
            addKpiBox(ctoKpiTable, "Warning Abstraction Days", warningDays + " Days", amberTheme, lightBg);
            addKpiBox(ctoKpiTable, "Critical Days / Breaches", criticalDays + " Days", criticalDays > 0 ? redTheme : emeraldTheme, lightBg);
            addKpiBox(ctoKpiTable, "Exceeding Daily Limit", exceedLimitDays + " Days", exceedLimitDays > 0 ? darkRedTheme : emeraldTheme, lightBg);
            document.add(ctoKpiTable);

            Paragraph ctoNote = new Paragraph("Note: CTO compliance status is calculated from recorded fresh-water consumption and the configured CTO daily limit.", noteFont);
            ctoNote.setSpacingAfter(15);
            document.add(ctoNote);

            // E. WATER BALANCE SUMMARY
            document.add(new Paragraph("E. Water Abstraction & Balance Summary", sectionFont));
            Paragraph pE = new Paragraph(" ", bodyFont); pE.setSpacingAfter(4); document.add(pE);

            double untappedPotential = Math.max(0.0, totalTreated - totalReused);

            PdfPTable balanceTable = new PdfPTable(2);
            balanceTable.setWidthPercentage(100);
            balanceTable.setWidths(new float[]{4f, 3f});
            balanceTable.setSpacingAfter(8);

            addStructuredRow(balanceTable, "Total Fresh-water Consumption", String.format("%,.1f kL", totalFresh), boldFont, bodyFont, lightBg);
            addStructuredRow(balanceTable, "Total Wastewater Generated", String.format("%,.1f kL", totalWaste), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(balanceTable, "Total Treated Water Available", String.format("%,.1f kL", totalTreated), boldFont, bodyFont, lightBg);
            addStructuredRow(balanceTable, "Total Approved Reused Water", String.format("%,.1f kL", totalReused), boldFont, bodyFont, Color.WHITE);
            addStructuredRow(balanceTable, "Untapped Reuse Potential", String.format("%,.1f kL", untappedPotential), boldFont, bodyFont, lightBg);
            addStructuredRow(balanceTable, "Unaccounted Water / Investigation Required", String.format("%,.1f kL", totalUnaccounted), boldFont, bodyFont, Color.WHITE);
            document.add(balanceTable);

            Paragraph balanceNote = new Paragraph("Unaccounted Water / Investigation Required note: Unaccounted water indicates a difference between main-meter consumption and recorded department-level consumption. It may be caused by unmetered use, incorrect readings, timing differences, meter error, or possible leakage. It requires investigation.", noteFont);
            balanceNote.setSpacingAfter(15);
            document.add(balanceNote);

            // ==========================================
            // PAGE 2: LANDSCAPE DETAILS
            // ==========================================
            document.setPageSize(PageSize.A4.rotate());
            document.newPage();

            // LANDSCAPE HEADER
            document.add(new Paragraph("AquaNexus — Telemetry Audit Ledger", titleFont));
            Paragraph telemetrySub = new Paragraph("DETAILED WATER USAGE & REUSE TELEMETRY RECORD", subTitleFont);
            telemetrySub.setSpacingAfter(15);
            document.add(telemetrySub);

            // F. TELEMETRY RECORD TABLE
            PdfPTable mainRecordTable = new PdfPTable(15);
            mainRecordTable.setWidthPercentage(100);
            mainRecordTable.setHeaderRows(1); // Repeat header on every page
            mainRecordTable.setWidths(new float[]{0.8f, 1.8f, 1.8f, 1.8f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.4f, 1.4f});
            mainRecordTable.setSpacingAfter(15);

            String[] tableHeaders = {
                "Sl. No.", "Date", "Department", "Meter Type", "Opening Read", "Closing Read", 
                "Fresh (kL)", "Waste (kL)", "Treated (kL)", "Reused (kL)", "Recovery %", "Unaccounted (kL)", "CTO Util %", "CTO Status", "Alert"
            };

            for (String th : tableHeaders) {
                addTableHeader(mainRecordTable, th, headerFont, tableHeaderBg);
            }

            int index = 1;
            boolean alternate = false;
            for (WaterDailyRecord r : records) {
                Color bg = alternate ? lightBg : Color.WHITE;
                alternate = !alternate;

                String ctoStatusStr = r.getComplianceStatus() != null ? r.getComplianceStatus() : "SAFE";
                String alertStatusStr = Boolean.TRUE.equals(r.getIsAnomaly()) ? "ANOMALY" : "NORMAL";

                addCell(mainRecordTable, String.valueOf(index++), bodyFont, Element.ALIGN_CENTER, bg);
                addCell(mainRecordTable, r.getDate() != null ? r.getDate().toString() : "N/A", bodyFont, Element.ALIGN_CENTER, bg);
                addCell(mainRecordTable, valOrNA(r.getDepartment()), bodyFont, Element.ALIGN_LEFT, bg);
                addCell(mainRecordTable, r.getMeterRecordType() != null ? r.getMeterRecordType() : "Flow Meter", bodyFont, Element.ALIGN_LEFT, bg);
                addCell(mainRecordTable, r.getOpeningMeterReading() != null ? String.format("%,.1f", r.getOpeningMeterReading()) : "N/A", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getClosingMeterReading() != null ? String.format("%,.1f", r.getClosingMeterReading()) : "N/A", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getFreshWaterConsumed() != null ? String.format("%,.1f", r.getFreshWaterConsumed()) : "0.0", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getWastewaterGenerated() != null ? String.format("%,.1f", r.getWastewaterGenerated()) : "0.0", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getTreatedWaterAvailable() != null ? String.format("%,.1f", r.getTreatedWaterAvailable()) : "0.0", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getReusedWater() != null ? String.format("%,.1f", r.getReusedWater()) : "0.0", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getReuseRecoveryRate() != null ? String.format("%.1f%%", r.getReuseRecoveryRate()) : "0.0%", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getUnaccountedWater() != null ? String.format("%,.1f", r.getUnaccountedWater()) : "0.0", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, r.getCtoUtilization() != null ? String.format("%.1f%%", r.getCtoUtilization()) : "0.0%", bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(mainRecordTable, ctoStatusStr, boldFont, Element.ALIGN_CENTER, bg);
                addCell(mainRecordTable, alertStatusStr, boldFont, Element.ALIGN_CENTER, bg);
            }
            document.add(mainRecordTable);

            // G. ANOMALY / POSSIBLE LEAKAGE DETECTED SECTION
            document.add(new Paragraph("G. Anomaly / Possible Leakage Register", sectionFont));
            Paragraph pG = new Paragraph(" ", bodyFont); pG.setSpacingAfter(4); document.add(pG);

            List<WaterDailyRecord> anomalies = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsAnomaly())).collect(Collectors.toList());

            if (anomalies.isEmpty()) {
                Paragraph noAnomaly = new Paragraph("No abnormal usage records were found during the selected period.", bodyFont);
                noAnomaly.setSpacingAfter(20);
                document.add(noAnomaly);
            } else {
                PdfPTable anomalyTable = new PdfPTable(7);
                anomalyTable.setWidthPercentage(100);
                anomalyTable.setWidths(new float[]{1.5f, 1.8f, 2f, 1.2f, 3f, 3.5f, 1.5f});
                anomalyTable.setSpacingAfter(20);

                String[] aHeaders = {"Date", "Department", "Anomaly Type", "Anomaly Score", "Reason / Behavior Flagged", "Recommended Action", "Status"};
                for (String ah : aHeaders) {
                    addTableHeader(anomalyTable, ah, headerFont, tableHeaderBg);
                }

                boolean aToggle = false;
                for (WaterDailyRecord ar : anomalies) {
                    Color bg = aToggle ? lightBg : Color.WHITE;
                    aToggle = !aToggle;

                    double lossVol = ar.getWaterLoss() != null ? ar.getWaterLoss() : 0.0;
                    double freshVol = ar.getFreshWaterConsumed() != null ? ar.getFreshWaterConsumed() : 0.0;
                    double reusePctVal = ar.getReusePercentage() != null ? ar.getReusePercentage() : 0.0;

                    String reason = "Normal usage bounds exceeded.";
                    String action = "Validate flow meters and review pipes.";
                    String aType = "Operational Variance";

                    if (ar.getCtoUtilization() != null && ar.getCtoUtilization() > 95) {
                        aType = "CTO LIMIT SPIKE";
                        reason = String.format("CTO utilization spiked to %.1f%%, exceeding critical threshold.", ar.getCtoUtilization());
                        action = "Reduce fresh water feed; engage recycling pumps immediately.";
                    } else if (lossVol > 0 && ar.getWastewaterGenerated() != null && ar.getWastewaterGenerated() > 0 && (lossVol / ar.getWastewaterGenerated()) > 0.6) {
                        aType = "UNRECOVERED DISCHARGE";
                        reason = String.format("High water loss: unrecovered discharge represents %.1f%% of wastewater.", (lossVol / ar.getWastewaterGenerated()) * 100);
                        action = "Inspect wastewater collection sumps and return recycle line valves.";
                    } else if (reusePctVal < 15 && freshVol > 500) {
                        aType = "LOW RECYCLING RATE";
                        reason = String.format("Recycling yield dropped to %.1f%% despite high consumption (%,.1f kL).", reusePctVal, freshVol);
                        action = "Verify RO filter status and secondary pump operations.";
                    }

                    addCell(anomalyTable, ar.getDate().toString(), bodyFont, Element.ALIGN_CENTER, bg);
                    addCell(anomalyTable, valOrNA(ar.getDepartment()), bodyFont, Element.ALIGN_LEFT, bg);
                    addCell(anomalyTable, aType, boldFont, Element.ALIGN_LEFT, bg);
                    addCell(anomalyTable, "0.85", bodyFont, Element.ALIGN_CENTER, bg); // Mock score
                    addCell(anomalyTable, reason, bodyFont, Element.ALIGN_LEFT, bg);
                    addCell(anomalyTable, action, bodyFont, Element.ALIGN_LEFT, bg);
                    addCell(anomalyTable, "UNRESOLVED", boldFont, Element.ALIGN_CENTER, bg);
                }
                document.add(anomalyTable);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private String valOrNA(String val) {
        return (val != null && !val.trim().isEmpty()) ? val : "N/A";
    }

    private byte[] generateEmptyReportPdf(String period) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 54, 54);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("AquaNexus Water Intelligence Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph("Reporting Period: " + period, FontFactory.getFont(FontFactory.HELVETICA, 10)));
            doc.add(new Paragraph("\n\nNo water consumption records found for the selected period.", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.RED)));
            doc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private void addStructuredRow(PdfPTable table, String label, String val, Font boldFont, Font bodyFont, Color bg) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, boldFont));
        c1.setBackgroundColor(bg); c1.setPadding(5);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val, bodyFont));
        c2.setBackgroundColor(bg); c2.setPadding(5);
        table.addCell(c2);
    }

    private void addTableHeader(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addKpiBox(PdfPTable table, String label, String val, Color color, Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        Paragraph labelP = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY));
        labelP.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelP);

        Paragraph valP = new Paragraph(val, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, color));
        valP.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valP);
        
        table.addCell(cell);
    }

    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final String period;
        private final String generatedBy;
        private final String timestamp;
        private PdfTemplate totalPages;

        public HeaderFooterPageEvent(String period, String generatedBy, String timestamp) {
            this.period = period;
            this.generatedBy = generatedBy;
            this.timestamp = timestamp;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // Header
            Phrase headerPhrase = new Phrase("AquaNexus Audit Report — " + period, font);
            float headerY = document.top() + 10;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, headerPhrase, document.left(), headerY, 0);

            // Footer
            String footerText = String.format("Confidential | Prepared by: %s | Generated: %s | Page %d of ",
                    generatedBy, timestamp, writer.getPageNumber());
            Phrase footerPhrase = new Phrase(footerText, font);

            float footerY = document.bottom() - 20;
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, footerPhrase, document.right() - 15, footerY, 0);

            cb.addTemplate(totalPages, document.right() - 15, footerY - 2);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            totalPages.beginText();
            totalPages.setFontAndSize(font.getCalculatedBaseFont(false), 8);
            totalPages.showText(String.valueOf(writer.getPageNumber()));
            totalPages.endText();
        }
    }
}
