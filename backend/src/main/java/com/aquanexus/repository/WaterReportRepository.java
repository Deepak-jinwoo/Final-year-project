package com.aquanexus.repository;

import com.aquanexus.model.WaterReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing generated PDF reports in MySQL.
 */
@Repository
public interface WaterReportRepository extends JpaRepository<WaterReportEntity, Long> {
    List<WaterReportEntity> findAllByOrderByGeneratedAtDesc();
}
