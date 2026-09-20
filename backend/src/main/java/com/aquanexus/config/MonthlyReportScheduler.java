package com.aquanexus.config;

import com.aquanexus.service.WaterMonthlyReportService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

/**
 * Automated Spring Scheduler for end-of-month monthly PDF report generation.
 */
@Configuration
@EnableScheduling
public class MonthlyReportScheduler {

    private final WaterMonthlyReportService monthlyReportService;

    public MonthlyReportScheduler(WaterMonthlyReportService monthlyReportService) {
        this.monthlyReportService = monthlyReportService;
    }

    /**
     * Automatically runs at 23:59 on the last day of every month to generate and archive PDF report.
     * Cron: "0 59 23 L * ?"
     */
    @Scheduled(cron = "0 59 23 L * ?")
    public void generateEndOfMonthReport() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.withDayOfMonth(1);
        monthlyReportService.generateMonthlyReport(start, end, null);
    }
}
