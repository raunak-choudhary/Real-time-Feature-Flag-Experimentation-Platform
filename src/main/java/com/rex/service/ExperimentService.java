package com.rex.service;

import com.rex.model.Experiment;
import com.rex.model.UserCohort;
import com.rex.repository.ExperimentRepository;
import com.rex.repository.UserCohortRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for A/B Testing Experiment management operations.
 * Provides comprehensive business logic for experiment lifecycle management,
 * user cohort assignment, and statistical analysis.
 *
 * Key Features:
 * - A/B testing experiment lifecycle management
 * - Sophisticated user cohort assignment algorithms
 * - Real-time experiment performance tracking
 * - Statistical significance monitoring
 * - Multi-variant experiment support
 */
@Service
@Transactional
public class ExperimentService {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    private final ExperimentRepository experimentRepository;
    private final UserCohortRepository userCohortRepository;

    @Autowired
    public ExperimentService(ExperimentRepository experimentRepository,
                             UserCohortRepository userCohortRepository) {
        this.experimentRepository = experimentRepository;
        this.userCohortRepository = userCohortRepository;
    }

    // ============================================
    // EXPERIMENT CRUD OPERATIONS
    // ============================================

    /**
     * Create a new A/B testing experiment with validation.
     */
    public Experiment createExperiment(String name, String description, String hypothesis,
                                       String controlVariantName, String testVariantName,
                                       Integer trafficPercentage, String environment, String createdBy) {
        logger.info("Creating new experiment: {} in environment: {}", name, environment);

        // Validation
        validateExperimentName(name);
        validateTrafficPercentage(trafficPercentage);
        validateVariantNames(controlVariantName, testVariantName);
        validateEnvironment(environment);

        // Check for duplicate names
        if (experimentRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    String.format("Experiment with name '%s' already exists", name)
            );
        }

        Experiment experiment = new Experiment(name, description, trafficPercentage,
                controlVariantName, testVariantName, createdBy);
        experiment.setHypothesis(hypothesis);
        experiment.setEnvironment(environment);
        experiment.setStatus(Experiment.ExperimentStatus.DRAFT);
        experiment.setConfidenceLevel(95.0); // Default confidence level

        Experiment savedExperiment = experimentRepository.save(experiment);
        logger.info("Successfully created experiment: {} with ID: {}", name, savedExperiment.getId());

        return savedExperiment;
    }

    /**
     * Create experiment with comprehensive configuration.
     */
    public Experiment createExperiment(String name, String description, String hypothesis,
                                       Integer trafficPercentage, String environment, String successMetric,
                                       Double expectedImprovement, Integer minimumSampleSize, String createdBy) {
        logger.info("Creating comprehensive experiment: {} with target sample size: {}", name, minimumSampleSize);

        validateExperimentName(name);
        validateTrafficPercentage(trafficPercentage);
        validateEnvironment(environment);

        if (experimentRepository.existsByName(name)) {
            throw new IllegalArgumentException("Experiment with name '" + name + "' already exists");
        }

        Experiment experiment = new Experiment(name, description, createdBy);
        experiment.setHypothesis(hypothesis);
        experiment.setTrafficPercentage(trafficPercentage);
        experiment.setEnvironment(environment);
        experiment.setSuccessMetric(successMetric);
        experiment.setExpectedImprovement(expectedImprovement);
        experiment.setMinimumSampleSize(minimumSampleSize);
        experiment.setStatus(Experiment.ExperimentStatus.DRAFT);

        return experimentRepository.save(experiment);
    }

    /**
     * Get experiment by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Experiment> getExperimentById(Long id) {
        logger.debug("Retrieving experiment by ID: {}", id);
        return experimentRepository.findById(id);
    }

    /**
     * Get experiment by name.
     */
    @Transactional(readOnly = true)
    public Optional<Experiment> getExperimentByName(String name) {
        logger.debug("Retrieving experiment by name: {}", name);
        return experimentRepository.findByName(name);
    }

    /**
     * Get all experiments.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getAllExperiments() {
        logger.debug("Retrieving all experiments");
        return experimentRepository.findAll();
    }

    /**
     * Get experiments by environment.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getExperimentsByEnvironment(String environment) {
        logger.debug("Retrieving experiments for environment: {}", environment);
        return experimentRepository.findByEnvironment(environment);
    }

    /**
     * Get experiments by status.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getExperimentsByStatus(Experiment.ExperimentStatus status) {
        logger.debug("Retrieving experiments with status: {}", status);
        return experimentRepository.findByStatus(status);
    }

    /**
     * Get running experiments.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getRunningExperiments() {
        logger.debug("Retrieving running experiments");
        return experimentRepository.findByStatusOrderByStartDateDesc(Experiment.ExperimentStatus.RUNNING);
    }

    /**
     * Get active experiments (running and not ended).
     */
    @Transactional(readOnly = true)
    public List<Experiment> getActiveExperiments() {
        logger.debug("Retrieving active experiments");
        return experimentRepository.findActiveExperiments();
    }

    /**
     * Update experiment.
     */
    public Experiment updateExperiment(Long id, String name, String description, String hypothesis,
                                       Integer trafficPercentage, String successMetric) {
        logger.info("Updating experiment ID: {}", id);

        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));

        // Can only update experiments in DRAFT status
        if (experiment.getStatus() != Experiment.ExperimentStatus.DRAFT) {
            throw new IllegalStateException("Can only update experiments in DRAFT status");
        }

        // Check for name conflicts (if name is being changed)
        if (!experiment.getName().equals(name) && experimentRepository.existsByName(name)) {
            throw new IllegalArgumentException("Experiment with name '" + name + "' already exists");
        }

        validateExperimentName(name);
        validateTrafficPercentage(trafficPercentage);

        experiment.setName(name);
        experiment.setDescription(description);
        experiment.setHypothesis(hypothesis);
        experiment.setTrafficPercentage(trafficPercentage);
        experiment.setSuccessMetric(successMetric);

        return experimentRepository.save(experiment);
    }

    /**
     * Delete experiment (soft delete by cancelling).
     */
    public void deleteExperiment(Long id) {
        logger.info("Cancelling experiment ID: {}", id);

        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));

        experiment.cancel(); // Soft delete - sets status to CANCELLED
        experimentRepository.save(experiment);

        logger.info("Successfully cancelled experiment: {}", experiment.getName());
    }

    // ============================================
    // EXPERIMENT LIFECYCLE MANAGEMENT
    // ============================================

    /**
     * Mark experiment as ready to start.
     */
    public Experiment markExperimentReady(Long id) {
        logger.info("Marking experiment ID: {} as ready", id);

        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));

        if (experiment.getStatus() != Experiment.ExperimentStatus.DRAFT) {
            throw new IllegalStateException("Can only mark DRAFT experiments as ready");
        }

        // Validate experiment is properly configured
        validateExperimentReadiness(experiment);

        experiment.markReady();
        Experiment savedExperiment = experimentRepository.save(experiment);

        logger.info("Successfully marked experiment '{}' as ready", experiment.getName());
        return savedExperiment;
    }

    /**
     * Start experiment.
     */
    public Experiment startExperiment(Long id) {
        logger.info("Starting experiment ID: {}", id);

        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));

        if (!experiment.canBeStarted()) {
            throw new IllegalStateException("Experiment cannot be started in current status: " + experiment.getStatus());
        }

        experiment.start();
        Experiment savedExperiment = experimentRepository.save(experiment);

        logger.info("Successfully started experiment: {}", experiment.getName());
        return savedExperiment;
    }

    /**
     * Pause experiment.
     */
    public Experiment pauseExperiment(Long id) {
        logger.info("Pausing experiment ID: {}", id);

        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));

        if (experiment.getStatus() != Experiment.ExperimentStatus.RUNNING) {
            throw new IllegalStateException("Can only pause RUNNING experiments");
        }

        experiment.pause();
        Experiment savedExperiment = experimentRepository.save(experiment);

        logger.info("Successfully paused experiment: {}", experiment.getName());
        return savedExperiment;
    }

    /**
     * Stop experiment.
     */
    public Experiment stopExperiment(Long id) {
        logger.info("Stopping experiment ID: {}", id);

        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));

        if (!experiment.isActive()) {
            throw new IllegalStateException("Can only stop active experiments");
        }

        experiment.stop();
        Experiment savedExperiment = experimentRepository.save(experiment);

        logger.info("Successfully stopped experiment: {}", experiment.getName());
        return savedExperiment;
    }

    /**
     * Archive experiment.
     */
    public Experiment archiveExperiment(Long id) {
        logger.info("Archiving experiment ID: {}", id);

        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));

        if (experiment.getStatus() != Experiment.ExperimentStatus.COMPLETED &&
                experiment.getStatus() != Experiment.ExperimentStatus.CANCELLED) {
            throw new IllegalStateException("Can only archive COMPLETED or CANCELLED experiments");
        }

        experiment.archive();
        Experiment savedExperiment = experimentRepository.save(experiment);

        logger.info("Successfully archived experiment: {}", experiment.getName());
        return savedExperiment;
    }

    // ============================================
    // USER COHORT ASSIGNMENT
    // ============================================

    /**
     * Assign user to experiment cohort using hash-based algorithm.
     * This ensures consistent assignment - same user always gets same variant.
     */
    public UserCohort assignUserToExperiment(String userId, Long experimentId, String sessionId) {
        logger.debug("Assigning user '{}' to experiment ID: {}", userId, experimentId);

        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + experimentId));

        // Check if experiment is running
        if (experiment.getStatus() != Experiment.ExperimentStatus.RUNNING) {
            throw new IllegalStateException("Cannot assign users to non-running experiment");
        }

        // Check if user is already assigned
        Optional<UserCohort> existingAssignment = userCohortRepository
                .findByUserIdAndExperimentId(userId, experimentId);

        if (existingAssignment.isPresent()) {
            logger.debug("User '{}' already assigned to experiment '{}'", userId, experiment.getName());
            return existingAssignment.get();
        }

        // Determine if user should be included based on traffic percentage
        if (!shouldUserBeIncluded(userId, experiment)) {
            logger.debug("User '{}' excluded from experiment '{}' due to traffic percentage",
                    userId, experiment.getName());
            return createExcludedCohort(userId, experiment, sessionId);
        }

        // Assign to control or treatment based on hash
        UserCohort.CohortType cohortType = determineCohortType(userId, experiment);
        String variantName = cohortType == UserCohort.CohortType.CONTROL ?
                experiment.getControlVariantName() : experiment.getTestVariantName();

        UserCohort cohort = new UserCohort(userId, sessionId, experiment, cohortType, variantName);
        cohort.setAssignmentMethod(UserCohort.AssignmentMethod.HASH_BASED);
        cohort.setAssignmentHash(calculateUserHash(userId, experiment.getName()));
        cohort.setEnvironment(experiment.getEnvironment());

        UserCohort savedCohort = userCohortRepository.save(cohort);

        // Increment experiment sample size
        experiment.incrementSampleSize();
        experimentRepository.save(experiment);

        logger.info("Successfully assigned user '{}' to experiment '{}' as {} ({})",
                userId, experiment.getName(), cohortType, variantName);

        return savedCohort;
    }

    /**
     * Assign user with specific assignment method.
     */
    public UserCohort assignUserToExperiment(String userId, Long experimentId,
                                             UserCohort.AssignmentMethod assignmentMethod,
                                             UserCohort.CohortType forcedCohortType) {
        logger.info("Manually assigning user '{}' to experiment ID: {} as {}",
                userId, experimentId, forcedCohortType);

        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + experimentId));

        // Check if user is already assigned
        Optional<UserCohort> existingAssignment = userCohortRepository
                .findByUserIdAndExperimentId(userId, experimentId);

        if (existingAssignment.isPresent()) {
            throw new IllegalStateException("User is already assigned to this experiment");
        }

        String variantName = forcedCohortType == UserCohort.CohortType.CONTROL ?
                experiment.getControlVariantName() : experiment.getTestVariantName();

        UserCohort cohort = new UserCohort(userId, experiment, forcedCohortType, variantName, assignmentMethod);
        cohort.setEnvironment(experiment.getEnvironment());

        UserCohort savedCohort = userCohortRepository.save(cohort);

        // Increment experiment sample size if not excluded
        if (forcedCohortType != UserCohort.CohortType.EXCLUDED) {
            experiment.incrementSampleSize();
            experimentRepository.save(experiment);
        }

        return savedCohort;
    }

    /**
     * Get user's assignment for an experiment.
     */
    @Transactional(readOnly = true)
    public Optional<UserCohort> getUserAssignment(String userId, Long experimentId) {
        return userCohortRepository.findByUserIdAndExperimentId(userId, experimentId);
    }

    /**
     * Check if user is assigned to experiment.
     */
    @Transactional(readOnly = true)
    public boolean isUserAssignedToExperiment(String userId, Long experimentId) {
        return userCohortRepository.existsByUserIdAndExperimentId(userId, experimentId);
    }

    /**
     * Get all assignments for a user.
     */
    @Transactional(readOnly = true)
    public List<UserCohort> getUserAssignments(String userId) {
        return userCohortRepository.findByUserId(userId);
    }

    /**
     * Get active assignments for a user.
     */
    @Transactional(readOnly = true)
    public List<UserCohort> getActiveUserAssignments(String userId) {
        return userCohortRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Record user exposure to experiment.
     */
    public UserCohort recordUserExposure(String userId, Long experimentId) {
        logger.debug("Recording exposure for user '{}' to experiment ID: {}", userId, experimentId);

        UserCohort cohort = userCohortRepository.findByUserIdAndExperimentId(userId, experimentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User is not assigned to this experiment. Cannot record exposure."));

        cohort.recordExposure();
        return userCohortRepository.save(cohort);
    }

    // ============================================
    // EXPERIMENT ANALYTICS
    // ============================================

    /**
     * Get experiment assignments statistics.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getExperimentAssignmentStats(Long experimentId) {
        logger.debug("Retrieving assignment statistics for experiment ID: {}", experimentId);
        return userCohortRepository.getAssignmentStats(experimentId);
    }

    /**
     * Get cohort distribution for experiment.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getCohortDistribution(Long experimentId) {
        logger.debug("Retrieving cohort distribution for experiment ID: {}", experimentId);
        return userCohortRepository.getCohortDistribution(experimentId);
    }

    /**
     * Get experiment completion percentage.
     */
    @Transactional(readOnly = true)
    public Double getExperimentCompletionPercentage(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + experimentId));

        return experiment.getCompletionPercentage();
    }

    /**
     * Check if experiment has reached minimum sample size.
     */
    @Transactional(readOnly = true)
    public boolean hasReachedMinimumSampleSize(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + experimentId));

        if (experiment.getMinimumSampleSize() == null) {
            return true; // No minimum requirement
        }

        return experiment.getCurrentSampleSize() >= experiment.getMinimumSampleSize();
    }

    /**
     * Get experiments needing more traffic.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getExperimentsNeedingMoreTraffic() {
        return experimentRepository.findExperimentsNeedingMoreTraffic();
    }

    /**
     * Get experiments needing completion.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getExperimentsNeedingCompletion() {
        return experimentRepository.findExperimentsNeedingCompletion();
    }

    // ============================================
    // EXPERIMENT DISCOVERY
    // ============================================

    /**
     * Get experiments eligible for a user.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getEligibleExperimentsForUser(String userId, String environment) {
        int userTrafficPercentile = calculateUserPercentile(userId);
        return experimentRepository.findEligibleExperimentsForUser(environment, userTrafficPercentile);
    }

    /**
     * Get ready experiments for environment.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getReadyExperiments(String environment) {
        return experimentRepository.findReadyExperiments(environment);
    }

    /**
     * Get recent experiments.
     */
    @Transactional(readOnly = true)
    public List<Experiment> getRecentExperiments(LocalDateTime since) {
        return experimentRepository.findRecentExperiments(since);
    }

    /**
     * Get experiment statistics by environment.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getExperimentStatsByEnvironment() {
        return experimentRepository.getExperimentStatsByEnvironment();
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    /**
     * Determine if user should be included in experiment based on traffic percentage.
     */
    private boolean shouldUserBeIncluded(String userId, Experiment experiment) {
        int userPercentile = calculateUserPercentile(userId);
        return userPercentile <= experiment.getTrafficPercentage();
    }

    /**
     * Determine cohort type (control vs treatment) for user.
     */
    private UserCohort.CohortType determineCohortType(String userId, Experiment experiment) {
        int hash = calculateUserHash(userId, experiment.getName());
        // 50/50 split between control and treatment
        return (hash % 2 == 0) ? UserCohort.CohortType.CONTROL : UserCohort.CohortType.TREATMENT;
    }

    /**
     * Create excluded cohort for user not included in traffic.
     */
    private UserCohort createExcludedCohort(String userId, Experiment experiment, String sessionId) {
        UserCohort cohort = new UserCohort(userId, sessionId, experiment,
                UserCohort.CohortType.EXCLUDED, "excluded");
        cohort.setAssignmentMethod(UserCohort.AssignmentMethod.HASH_BASED);
        cohort.setEnvironment(experiment.getEnvironment());
        return userCohortRepository.save(cohort);
    }

    /**
     * Calculate user percentile for traffic allocation (1-100).
     */
    private int calculateUserPercentile(String userId) {
        int hash = Math.abs(userId.hashCode());
        return hash % 100 + 1;
    }

    /**
     * Calculate user hash for consistent assignment.
     */
    private int calculateUserHash(String userId, String context) {
        String combined = userId + ":" + context;
        return Math.abs(combined.hashCode());
    }

    /**
     * Validate experiment readiness before marking as ready.
     */
    private void validateExperimentReadiness(Experiment experiment) {
        if (experiment.getName() == null || experiment.getName().trim().isEmpty()) {
            throw new IllegalStateException("Experiment name is required");
        }
        if (experiment.getControlVariantName() == null || experiment.getTestVariantName() == null) {
            throw new IllegalStateException("Both control and test variant names are required");
        }
        if (experiment.getTrafficPercentage() == null || experiment.getTrafficPercentage() <= 0) {
            throw new IllegalStateException("Valid traffic percentage is required");
        }
    }

    // ============================================
    // VALIDATION METHODS
    // ============================================

    /**
     * Validate experiment name.
     */
    private void validateExperimentName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Experiment name cannot be null or empty");
        }
        if (name.length() < 3 || name.length() > 100) {
            throw new IllegalArgumentException("Experiment name must be between 3 and 100 characters");
        }
    }

    /**
     * Validate traffic percentage.
     */
    private void validateTrafficPercentage(Integer percentage) {
        if (percentage == null) {
            throw new IllegalArgumentException("Traffic percentage cannot be null");
        }
        if (percentage < 1 || percentage > 100) {
            throw new IllegalArgumentException("Traffic percentage must be between 1 and 100");
        }
    }

    /**
     * Validate variant names.
     */
    private void validateVariantNames(String controlVariantName, String testVariantName) {
        if (controlVariantName == null || controlVariantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Control variant name cannot be null or empty");
        }
        if (testVariantName == null || testVariantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Test variant name cannot be null or empty");
        }
        if (controlVariantName.equals(testVariantName)) {
            throw new IllegalArgumentException("Control and test variant names must be different");
        }
    }

    /**
     * Validate environment.
     */
    private void validateEnvironment(String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment cannot be null or empty");
        }
    }

    // ============================================
    // EXISTENCE CHECKS
    // ============================================

    /**
     * Check if experiment exists by name.
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return experimentRepository.existsByName(name);
    }

    /**
     * Check if experiment exists by ID.
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return experimentRepository.existsById(id);
    }

    /**
     * Get experiment count by status.
     */
    @Transactional(readOnly = true)
    public long getCountByStatus(Experiment.ExperimentStatus status) {
        return experimentRepository.countByStatus(status);
    }
}