package com.aquanexus.repository;

import com.aquanexus.model.WaterDailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WaterDailyRecordRepository extends JpaRepository<WaterDailyRecord, Long> {

    List<WaterDailyRecord> findTop14ByIndustryIdOrderByDateDesc(Integer industryId);

    List<WaterDailyRecord> findByIndustryIdAndDateBetweenOrderByDateAsc(
            Integer industryId, LocalDate startDate, LocalDate endDate);

    List<WaterDailyRecord> findByIndustryIdOrderByDateAsc(Integer industryId);

    List<WaterDailyRecord> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);

    List<WaterDailyRecord> findByDepartmentAndDateBetweenOrderByDateAsc(
            String department, LocalDate start, LocalDate end);

    @Query("SELECT r FROM WaterDailyRecord r ORDER BY r.date DESC")
    List<WaterDailyRecord> findAllOrderByDateDesc();

    @Query("SELECT COALESCE(SUM(r.freshWaterConsumed), 0) FROM WaterDailyRecord r WHERE r.date BETWEEN :start AND :end")
    Double sumFreshWaterBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(r.reusedWater), 0) FROM WaterDailyRecord r WHERE r.date BETWEEN :start AND :end")
    Double sumReusedWaterBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(r.wastewaterGenerated), 0) FROM WaterDailyRecord r WHERE r.date BETWEEN :start AND :end")
    Double sumWastewaterBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(r.waterLoss), 0) FROM WaterDailyRecord r WHERE r.date BETWEEN :start AND :end")
    Double sumWaterLossBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(r) FROM WaterDailyRecord r WHERE r.isAnomaly = true AND r.date BETWEEN :start AND :end")
    Long countAnomaliesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT DISTINCT r.department FROM WaterDailyRecord r WHERE r.department IS NOT NULL")
    List<String> findDistinctDepartments();
}
