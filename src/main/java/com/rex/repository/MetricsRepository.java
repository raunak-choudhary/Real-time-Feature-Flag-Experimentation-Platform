package com.rex.repository;

import com.rex.model.Metrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Metrics entity operations.
 * Provides CRUD operations and custom queries for analytics and event tracking.
 * FIXED: Corrected relationship navigation for featureFlag and experiment entities.
 */
@Repository
public interface MetricsRepository extends JpaRepository<Metrics, Long> {

    /**
     * Find metrics by user ID.
     */
    List<Metrics> findByUserId(String userId);

    /**
     * Find metrics by user ID and event type.
     */
    List<Metrics> findByUserIdAndEventType(String userId, Metrics.EventType eventType);

    /**
     * Find metrics by experiment (using relationship navigation).
     */
    List<Metrics> findByExperiment_Id(Long experimentId);

    /**
     * Find metrics by feature flag (using relationship navigation).
     */
    List<Metrics> findByFeatureFlag_Id(Long featureFlagId);

    /**
     * Find metrics by event type.
     */
    List<Metrics> findByEventType(Metrics.EventType eventType);

    /**
     * Find metrics by environment.
     */
    List<Metrics> findByEnvironment(String environment);

    /**
     * Find metrics by variant name (for experiment analysis).
     */
    List<Metrics> findByVariantName(String variantName);

    /**
     * Find metrics within date range.
     */
    @Query("SELECT m FROM Metrics m WHERE m.timestamp >= :startDate AND m.timestamp <= :endDate ORDER BY m.timestamp DESC")
    List<Metrics> findByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Count metrics by event type.
     */
    long countByEventType(Metrics.EventType eventType);

    /**
     * Count metrics by experiment (using relationship navigation).
     */
    long countByExperiment_Id(Long experimentId);

    /**
     * Count metrics by feature flag (using relationship navigation).
     */
    long countByFeatureFlag_Id(Long featureFlagId);

    /**
     * Get experiment performance metrics.
     */
    @Query("""
        SELECT m.variantName,
               COUNT(*) as totalEvents,
               COUNT(CASE WHEN m.eventType = 'CONVERSION' THEN 1 END) as conversions,
               COUNT(DISTINCT m.userId) as uniqueUsers,
               AVG(m.eventValue) as avgEventValue,
               SUM(m.revenue) as totalRevenue
        FROM Metrics m 
        WHERE m.experiment.id = :experimentId
        GROUP BY m.variantName
        ORDER BY m.variantName
        """)
    List<Object[]> getExperimentPerformance(@Param("experimentId") Long experimentId);

    /**
     * Get feature flag usage statistics.
     */
    @Query("""
        SELECT m.featureFlag.id,
               COUNT(*) as totalExposures,
               COUNT(DISTINCT m.userId) as uniqueUsers,
               COUNT(CASE WHEN m.eventType = 'FLAG_ENABLED' THEN 1 END) as enabledEvents,
               COUNT(CASE WHEN m.eventType = 'FLAG_DISABLED' THEN 1 END) as disabledEvents
        FROM Metrics m 
        WHERE m.featureFlag.id = :flagId
        GROUP BY m.featureFlag.id
        """)
    List<Object[]> getFeatureFlagUsage(@Param("flagId") Long flagId);

    /**
     * Get conversion funnel for experiment.
     */
    @Query("""
        SELECT m.eventType, m.variantName, COUNT(*) as eventCount
        FROM Metrics m 
        WHERE m.experiment.id = :experimentId 
        AND m.eventType IN ('EXPERIMENT_EXPOSURE', 'CLICK', 'CONVERSION')
        GROUP BY m.eventType, m.variantName
        ORDER BY m.variantName, 
                 CASE m.eventType 
                     WHEN 'EXPERIMENT_EXPOSURE' THEN 1 
                     WHEN 'CLICK' THEN 2 
                     WHEN 'CONVERSION' THEN 3 
                 END
        """)
    List<Object[]> getConversionFunnel(@Param("experimentId") Long experimentId);

    /**
     * Get hourly metrics for dashboard charts.
     * H2-compatible version using FORMATDATETIME function.
     */
    @Query("""
        SELECT FORMATDATETIME(m.timestamp, 'yyyy-MM-dd HH') as hour,
               COUNT(*) as eventCount,
               COUNT(DISTINCT m.userId) as uniqueUsers
        FROM Metrics m 
        WHERE m.timestamp >= :startDate 
        AND m.environment = :environment
        GROUP BY FORMATDATETIME(m.timestamp, 'yyyy-MM-dd HH')
        ORDER BY FORMATDATETIME(m.timestamp, 'yyyy-MM-dd HH') DESC
        """)
    List<Object[]> getHourlyMetrics(@Param("startDate") LocalDateTime startDate,
                                    @Param("environment") String environment);

    /**
     * Get daily metrics aggregation.
     * H2-compatible version using FORMATDATETIME function.
     */
    @Query("""
        SELECT FORMATDATETIME(m.timestamp, 'yyyy-MM-dd') as day,
               m.eventType,
               COUNT(*) as eventCount,
               COUNT(DISTINCT m.userId) as uniqueUsers,
               AVG(m.eventValue) as avgValue
        FROM Metrics m 
        WHERE m.timestamp >= :startDate
        GROUP BY FORMATDATETIME(m.timestamp, 'yyyy-MM-dd'), m.eventType
        ORDER BY FORMATDATETIME(m.timestamp, 'yyyy-MM-dd') DESC, m.eventType
        """)
    List<Object[]> getDailyMetrics(@Param("startDate") LocalDateTime startDate);

    /**
     * Get top performing variants across all experiments.
     * H2-compatible version with explicit ORDER BY calculation.
     */
    @Query("""
        SELECT m.experiment.id, m.variantName,
               COUNT(CASE WHEN m.eventType = 'CONVERSION' THEN 1 END) as conversions,
               COUNT(CASE WHEN m.eventType = 'EXPERIMENT_EXPOSURE' THEN 1 END) as exposures,
               CASE 
                   WHEN COUNT(CASE WHEN m.eventType = 'EXPERIMENT_EXPOSURE' THEN 1 END) > 0 
                   THEN CAST(COUNT(CASE WHEN m.eventType = 'CONVERSION' THEN 1 END) AS DOUBLE) / 
                        COUNT(CASE WHEN m.eventType = 'EXPERIMENT_EXPOSURE' THEN 1 END) * 100
                   ELSE 0 
               END as conversionRate
        FROM Metrics m 
        WHERE m.experiment IS NOT NULL
        GROUP BY m.experiment.id, m.variantName
        HAVING COUNT(CASE WHEN m.eventType = 'EXPERIMENT_EXPOSURE' THEN 1 END) >= :minExposures
        ORDER BY CASE 
                     WHEN COUNT(CASE WHEN m.eventType = 'EXPERIMENT_EXPOSURE' THEN 1 END) > 0 
                     THEN CAST(COUNT(CASE WHEN m.eventType = 'CONVERSION' THEN 1 END) AS DOUBLE) / 
                          COUNT(CASE WHEN m.eventType = 'EXPERIMENT_EXPOSURE' THEN 1 END) * 100
                     ELSE 0 
                 END DESC
        """)
    List<Object[]> getTopPerformingVariants(@Param("minExposures") Long minExposures);

    /**
     * Get error metrics for monitoring.
     */
    @Query("""
        SELECT m.eventName,
               COUNT(*) as errorCount,
               COUNT(DISTINCT m.userId) as affectedUsers,
               m.environment
        FROM Metrics m 
        WHERE m.eventType = 'ERROR' 
        AND m.timestamp >= :sinceDate
        GROUP BY m.eventName, m.environment
        ORDER BY errorCount DESC
        """)
    List<Object[]> getErrorMetrics(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Get user engagement metrics.
     */
    @Query("""
        SELECT m.userId,
               COUNT(*) as totalEvents,
               COUNT(DISTINCT m.experiment.id) as experimentsParticipated,
               COUNT(DISTINCT m.featureFlag.id) as flagsExposed,
               MAX(m.timestamp) as lastActivity
        FROM Metrics m 
        WHERE m.timestamp >= :sinceDate
        GROUP BY m.userId
        HAVING COUNT(*) >= :minEvents
        ORDER BY totalEvents DESC
        """)
    List<Object[]> getUserEngagement(@Param("sinceDate") LocalDateTime sinceDate,
                                     @Param("minEvents") Long minEvents);

    /**
     * Get platform/device distribution.
     */
    @Query("""
        SELECT m.platform, m.deviceType,
               COUNT(*) as eventCount,
               COUNT(DISTINCT m.userId) as uniqueUsers
        FROM Metrics m 
        WHERE m.timestamp >= :sinceDate
        GROUP BY m.platform, m.deviceType
        ORDER BY eventCount DESC
        """)
    List<Object[]> getPlatformDistribution(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Get revenue metrics by experiment variant.
     */
    @Query("""
        SELECT m.experiment.id, m.variantName,
               SUM(m.revenue) as totalRevenue,
               AVG(m.revenue) as avgRevenue,
               COUNT(CASE WHEN m.revenue > 0 THEN 1 END) as revenueEvents
        FROM Metrics m 
        WHERE m.revenue IS NOT NULL AND m.revenue > 0
        GROUP BY m.experiment.id, m.variantName
        ORDER BY totalRevenue DESC
        """)
    List<Object[]> getRevenueMetrics();

    /**
     * Find recent high-value events.
     */
    @Query("""
        SELECT m FROM Metrics m 
        WHERE m.eventValue >= :minValue 
        AND m.timestamp >= :sinceDate
        ORDER BY m.eventValue DESC, m.timestamp DESC
        """)
    List<Metrics> findHighValueEvents(@Param("minValue") Double minValue,
                                      @Param("sinceDate") LocalDateTime sinceDate);
}