package com.rex.repository;

import com.rex.model.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Experiment entity operations.
 * Provides CRUD operations and custom queries for A/B testing experiment management.
 * FIXED: Verified all enum references and query syntax for proper compilation.
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    /**
     * Find experiment by name (unique constraint).
     */
    Optional<Experiment> findByName(String name);

    /**
     * Find all experiments by status.
     */
    List<Experiment> findByStatus(Experiment.ExperimentStatus status);

    /**
     * Find experiments by environment.
     */
    List<Experiment> findByEnvironment(String environment);

    /**
     * Find experiments by status and environment.
     */
    List<Experiment> findByStatusAndEnvironment(Experiment.ExperimentStatus status, String environment);

    /**
     * Find experiments created by specific user.
     */
    List<Experiment> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Find all experiments by status ordered by start date.
     */
    List<Experiment> findByStatusOrderByStartDateDesc(Experiment.ExperimentStatus status);

    /**
     * Count experiments by status.
     */
    long countByStatus(Experiment.ExperimentStatus status);

    /**
     * Check if experiment exists by name.
     */
    boolean existsByName(String name);

    /**
     * Find experiments within date range.
     */
    @Query("SELECT e FROM Experiment e WHERE e.startDate >= :startDate AND e.startDate <= :endDate")
    List<Experiment> findByStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Find currently active experiments (running and not ended).
     */
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.status = 'RUNNING' 
        AND (e.endDate IS NULL OR e.endDate > CURRENT_TIMESTAMP)
        ORDER BY e.startDate DESC
        """)
    List<Experiment> findActiveExperiments();

    /**
     * Find experiments ready to start.
     */
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.status = 'READY' 
        AND e.environment = :environment
        ORDER BY e.createdAt ASC
        """)
    List<Experiment> findReadyExperiments(@Param("environment") String environment);

    /**
     * Find experiments that need attention (running but should have ended).
     */
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.status = 'RUNNING' 
        AND e.endDate IS NOT NULL 
        AND e.endDate < CURRENT_TIMESTAMP
        """)
    List<Experiment> findExperimentsNeedingCompletion();

    /**
     * Get experiment statistics by environment.
     */
    @Query("""
        SELECT e.environment,
               COUNT(*) as total,
               SUM(CASE WHEN e.status = 'RUNNING' THEN 1 ELSE 0 END) as running,
               SUM(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
               SUM(CASE WHEN e.status = 'DRAFT' THEN 1 ELSE 0 END) as draft
        FROM Experiment e 
        GROUP BY e.environment
        """)
    List<Object[]> getExperimentStatsByEnvironment();

    /**
     * Find experiments with low sample size that need more traffic.
     */
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.status = 'RUNNING' 
        AND e.currentSampleSize < e.minimumSampleSize
        ORDER BY (CAST(e.currentSampleSize AS double) / e.minimumSampleSize) ASC
        """)
    List<Experiment> findExperimentsNeedingMoreTraffic();

    /**
     * Find experiments by traffic percentage range.
     */
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.trafficPercentage >= :minPercentage 
        AND e.trafficPercentage <= :maxPercentage
        ORDER BY e.trafficPercentage DESC
        """)
    List<Experiment> findByTrafficPercentageRange(@Param("minPercentage") Integer minPercentage,
                                                  @Param("maxPercentage") Integer maxPercentage);

    /**
     * Find recent experiments for dashboard.
     */
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.createdAt >= :sinceDate 
        ORDER BY e.createdAt DESC
        """)
    List<Experiment> findRecentExperiments(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find experiments by success metric.
     */
    List<Experiment> findBySuccessMetric(String successMetric);

    /**
     * Custom query to find experiments that a user might be eligible for.
     */
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.status = 'RUNNING'
        AND e.environment = :environment
        AND (e.endDate IS NULL OR e.endDate > CURRENT_TIMESTAMP)
        AND e.trafficPercentage >= :userTrafficPercentile
        ORDER BY e.startDate DESC
        """)
    List<Experiment> findEligibleExperimentsForUser(
            @Param("environment") String environment,
            @Param("userTrafficPercentile") Integer userTrafficPercentile
    );
}