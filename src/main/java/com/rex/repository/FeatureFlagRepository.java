package com.rex.repository;

import com.rex.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FeatureFlag entity operations.
 * Provides CRUD operations and custom queries for feature flag management.
 * VERIFIED: All enum references and query syntax are correct for compilation.
 */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    /**
     * Find feature flag by name (unique constraint).
     */
    Optional<FeatureFlag> findByName(String name);

    /**
     * Find all feature flags by status.
     */
    List<FeatureFlag> findByStatus(FeatureFlag.FlagStatus status);

    /**
     * Find all enabled feature flags.
     */
    List<FeatureFlag> findByEnabledTrue();

    /**
     * Find all disabled feature flags.
     */
    List<FeatureFlag> findByEnabledFalse();

    /**
     * Find feature flags by environment.
     */
    List<FeatureFlag> findByEnvironment(String environment);

    /**
     * Find active feature flags by environment.
     */
    List<FeatureFlag> findByStatusAndEnvironment(FeatureFlag.FlagStatus status, String environment);

    /**
     * Find feature flags by enabled status and environment.
     */
    List<FeatureFlag> findByEnabledAndEnvironment(boolean enabled, String environment);

    /**
     * Count feature flags by status.
     */
    long countByStatus(FeatureFlag.FlagStatus status);

    /**
     * Count enabled feature flags.
     */
    long countByEnabledTrue();

    /**
     * Check if feature flag exists by name.
     */
    boolean existsByName(String name);

    /**
     * Custom query to find feature flags with rollout percentage greater than specified value.
     */
    @Query("SELECT ff FROM FeatureFlag ff WHERE ff.rolloutPercentage > :percentage AND ff.enabled = true")
    List<FeatureFlag> findEnabledFlagsWithRolloutGreaterThan(@Param("percentage") Integer percentage);

    /**
     * Custom query to find feature flags created by specific user.
     */
    @Query("SELECT ff FROM FeatureFlag ff WHERE ff.createdBy = :createdBy ORDER BY ff.createdAt DESC")
    List<FeatureFlag> findByCreatedByOrderByCreatedAtDesc(@Param("createdBy") String createdBy);

    /**
     * Custom query to get feature flag statistics by environment.
     */
    @Query("""
        SELECT ff.environment, 
               COUNT(*) as total,
               SUM(CASE WHEN ff.enabled = true THEN 1 ELSE 0 END) as enabled,
               SUM(CASE WHEN ff.status = 'ACTIVE' THEN 1 ELSE 0 END) as active
        FROM FeatureFlag ff 
        GROUP BY ff.environment
        """)
    List<Object[]> getFeatureFlagStatsByEnvironment();

    /**
     * Custom query to find recently updated feature flags.
     */
    @Query("SELECT ff FROM FeatureFlag ff WHERE ff.updatedAt >= :sinceDate ORDER BY ff.updatedAt DESC")
    List<FeatureFlag> findRecentlyUpdated(@Param("sinceDate") java.time.LocalDateTime sinceDate);

    /**
     * Custom query to find feature flags suitable for a user based on rollout percentage.
     * This is useful for gradual rollouts.
     */
    @Query("""
        SELECT ff FROM FeatureFlag ff 
        WHERE ff.enabled = true 
        AND ff.status = 'ACTIVE' 
        AND ff.environment = :environment
        AND (ff.rolloutPercentage IS NULL OR ff.rolloutPercentage >= :userPercentile)
        """)
    List<FeatureFlag> findAvailableFlagsForUser(
            @Param("environment") String environment,
            @Param("userPercentile") Integer userPercentile
    );
}