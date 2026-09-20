package com.aquanexus.repository;

import com.aquanexus.model.WaterAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterAlertRepository extends JpaRepository<WaterAlert, Long> {

    List<WaterAlert> findByIndustryIdOrderByCreatedAtDesc(Integer industryId);

    List<WaterAlert> findByIsResolvedFalseOrderByCreatedAtDesc();

    List<WaterAlert> findByDateBetweenOrderByCreatedAtDesc(LocalDate start, LocalDate end);

    long countByIsResolvedFalse();

    long countByIsResolvedFalseAndSeverity(String severity);

    Optional<WaterAlert> findFirstByIndustryIdAndDepartmentAndDateAndAlertTypeOrderByCreatedAtDesc(
            Integer industryId, String department, LocalDate date, String alertType);

    @Query("SELECT a FROM WaterAlert a WHERE " +
           "(:industryId IS NULL OR a.industryId = :industryId) AND " +
           "(:startDate IS NULL OR a.date >= :startDate) AND " +
           "(:endDate IS NULL OR a.date <= :endDate) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:alertType IS NULL OR a.alertType = :alertType) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:department IS NULL OR a.department = :department) " +
           "ORDER BY a.date DESC, a.createdAt DESC")
    List<WaterAlert> findFilteredAlerts(
            @Param("industryId") Integer industryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("severity") String severity,
            @Param("alertType") String alertType,
            @Param("status") String status,
            @Param("department") String department);

    List<WaterAlert> findTop10ByIsResolvedFalseOrderByCreatedAtDesc();

    List<WaterAlert> findByRecordId(Long recordId);
}
