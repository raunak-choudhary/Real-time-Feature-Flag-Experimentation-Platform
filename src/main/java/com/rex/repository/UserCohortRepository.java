package com.rex.repository;

import com.rex.model.UserCohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserCohort entity operations.
 * Provides CRUD operations and custom queries for user experiment assignment management.
 * FIXED: Corrected relationship navigation for experiment entities.
 */
@Repository
public interface UserCohortRepository extends JpaRepository<UserCohort, Long> {

    /**
     * Find user's assignment for a specific experiment.
     */
    @Query("SELECT uc FROM UserCohort uc WHERE uc.userId = :userId AND uc.experiment.id = :experimentId")
    Optional<UserCohort> findByUserIdAndExperimentId(@Param("userId") String userId, @Param("experimentId") Long experimentId);

    /**
     * Find all assignments for a user.
     */
    List<UserCohort> findByUserId(String userId);

    /**
     * Find all assignments for a user in specific environment.
     */
    List<UserCohort> findByUserIdAndEnvironment(String userId, String environment);

    /**
     * Find all active assignments for a user.
     */
    List<UserCohort> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Find all assignments for an experiment (using relationship navigation).
     */
    List<UserCohort> findByExperiment_Id(Long experimentId);

    /**
     * Find assignments by experiment and cohort type.
     */
    @Query("SELECT uc FROM UserCohort uc WHERE uc.experiment.id = :experimentId AND uc.cohortType = :cohortType")
    List<UserCohort> findByExperimentIdAndCohortType(@Param("experimentId") Long experimentId, @Param("cohortType") UserCohort.CohortType cohortType);

    /**
     * Find assignments by experiment and variant name.
     */
    @Query("SELECT uc FROM UserCohort uc WHERE uc.experiment.id = :experimentId AND uc.variantName = :variantName")
    List<UserCohort> findByExperimentIdAndVariantName(@Param("experimentId") Long experimentId, @Param("variantName") String variantName);

    /**
     * Find assignments by assignment method.
     */
    List<UserCohort> findByAssignmentMethod(UserCohort.AssignmentMethod assignmentMethod);

    /**
     * Count assignments by experiment (using relationship navigation).
     */
    long countByExperiment_Id(Long experimentId);

    /**
     * Count assignments by experiment and cohort type.
     */
    @Query("SELECT COUNT(uc) FROM UserCohort uc WHERE uc.experiment.id = :experimentId AND uc.cohortType = :cohortType")
    long countByExperimentIdAndCohortType(@Param("experimentId") Long experimentId, @Param("cohortType") UserCohort.CohortType cohortType);

    /**
     * Count active assignments by experiment.
     */
    @Query("SELECT COUNT(uc) FROM UserCohort uc WHERE uc.experiment.id = :experimentId AND uc.isActive = true")
    long countByExperimentIdAndIsActiveTrue(@Param("experimentId") Long experimentId);

    /**
     * Check if user is assigned to experiment.
     */
    @Query("SELECT COUNT(uc) > 0 FROM UserCohort uc WHERE uc.userId = :userId AND uc.experiment.id = :experimentId")
    boolean existsByUserIdAndExperimentId(@Param("userId") String userId, @Param("experimentId") Long experimentId);

    /**
     * Find assignments with high exposure count (engaged users).
     */
    @Query("SELECT uc FROM UserCohort uc WHERE uc.exposureCount >= :minExposure ORDER BY uc.exposureCount DESC")
    List<UserCohort> findHighlyEngagedUsers(@Param("minExposure") Integer minExposure);

    /**
     * Get cohort distribution for an experiment.
     */
    @Query("""
        SELECT uc.cohortType, uc.variantName, COUNT(*) as count
        FROM UserCohort uc 
        WHERE uc.experiment.id = :experimentId
        GROUP BY uc.cohortType, uc.variantName
        ORDER BY COUNT(*) DESC
        """)
    List<Object[]> getCohortDistribution(@Param("experimentId") Long experimentId);

    /**
     * Get assignment statistics by experiment.
     */
    @Query("""
        SELECT uc.experiment.id,
               COUNT(*) as totalAssignments,
               COUNT(CASE WHEN uc.cohortType = 'CONTROL' THEN 1 END) as controlCount,
               COUNT(CASE WHEN uc.cohortType = 'TREATMENT' THEN 1 END) as treatmentCount,
               COUNT(CASE WHEN uc.isActive = true THEN 1 END) as activeCount,
               AVG(uc.exposureCount) as avgExposure
        FROM UserCohort uc 
        WHERE uc.experiment.id = :experimentId
        GROUP BY uc.experiment.id
        """)
    List<Object[]> getAssignmentStats(@Param("experimentId") Long experimentId);

    /**
     * Find users assigned within date range.
     */
    @Query("""
        SELECT uc FROM UserCohort uc 
        WHERE uc.assignedAt >= :startDate AND uc.assignedAt <= :endDate
        ORDER BY uc.assignedAt DESC
        """)
    List<UserCohort> findAssignmentsInDateRange(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Find assignments that haven't been exposed recently.
     */
    @Query("""
        SELECT uc FROM UserCohort uc 
        WHERE uc.isActive = true 
        AND (uc.lastExposureAt IS NULL OR uc.lastExposureAt < :cutoffDate)
        ORDER BY uc.lastExposureAt ASC NULLS FIRST
        """)
    List<UserCohort> findStaleAssignments(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find assignments by session ID (useful for session-based experiments).
     */
    List<UserCohort> findBySessionId(String sessionId);

    /**
     * Get assignment method distribution.
     */
    @Query("""
        SELECT uc.assignmentMethod, COUNT(*) as count
        FROM UserCohort uc 
        GROUP BY uc.assignmentMethod
        ORDER BY COUNT(*) DESC
        """)
    List<Object[]> getAssignmentMethodDistribution();

    /**
     * Find users with multiple active assignments (potential conflicts).
     */
    @Query("""
        SELECT uc.userId, COUNT(*) as assignmentCount
        FROM UserCohort uc 
        WHERE uc.isActive = true 
        GROUP BY uc.userId 
        HAVING COUNT(*) > 1
        ORDER BY COUNT(*) DESC
        """)
    List<Object[]> findUsersWithMultipleAssignments();

    /**
     * Get environment-wise assignment statistics.
     */
    @Query("""
        SELECT uc.environment,
               COUNT(*) as totalAssignments,
               COUNT(DISTINCT uc.userId) as uniqueUsers,
               COUNT(DISTINCT uc.experiment.id) as activeExperiments
        FROM UserCohort uc 
        WHERE uc.isActive = true
        GROUP BY uc.environment
        """)
    List<Object[]> getEnvironmentStats();

    /**
     * Find recently assigned users for an experiment.
     */
    @Query("""
        SELECT uc FROM UserCohort uc 
        WHERE uc.experiment.id = :experimentId 
        AND uc.assignedAt >= :sinceDate
        ORDER BY uc.assignedAt DESC
        """)
    List<UserCohort> findRecentAssignments(@Param("experimentId") Long experimentId,
                                           @Param("sinceDate") LocalDateTime sinceDate);
}