package com.rex.service;

import com.rex.model.FeatureFlag;
import com.rex.repository.FeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Feature Flag management operations.
 * Provides comprehensive business logic for real-time feature flag management,
 * including CRUD operations, toggle functionality, and rollout management.
 *
 * Key Features:
 * - Real-time flag toggling without deployment
 * - Gradual rollout percentage management
 * - Environment-specific flag handling
 * - Validation and error handling
 * - Audit trail support
 */
@Service
@Transactional
public class FeatureFlagService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlagRepository featureFlagRepository;

    @Autowired
    public FeatureFlagService(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    // ============================================
    // CRUD OPERATIONS
    // ============================================

    /**
     * Create a new feature flag with validation.
     * Ensures flag names are unique per environment.
     */
    public FeatureFlag createFeatureFlag(String name, String description, String environment, String createdBy) {
        logger.info("Creating new feature flag: {} in environment: {}", name, environment);

        // Validation
        validateFlagName(name);
        validateEnvironment(environment);

        // Check for duplicate names in same environment
        if (existsByNameAndEnvironment(name, environment)) {
            throw new IllegalArgumentException(
                    String.format("Feature flag with name '%s' already exists in environment '%s'", name, environment)
            );
        }

        FeatureFlag flag = new FeatureFlag(name, description, createdBy);
        flag.setEnvironment(environment);
        flag.setEnabled(false); // Start disabled by default
        flag.setStatus(FeatureFlag.FlagStatus.INACTIVE);
        flag.setRolloutPercentage(0); // Start with 0% rollout

        FeatureFlag savedFlag = featureFlagRepository.save(flag);
        logger.info("Successfully created feature flag: {} with ID: {}", name, savedFlag.getId());

        return savedFlag;
    }

    /**
     * Create feature flag with all parameters.
     */
    public FeatureFlag createFeatureFlag(String name, String description, Boolean enabled,
                                         FeatureFlag.FlagStatus status, String environment,
                                         Integer rolloutPercentage, String createdBy) {
        logger.info("Creating feature flag: {} with status: {} and rollout: {}%", name, status, rolloutPercentage);

        validateFlagName(name);
        validateEnvironment(environment);
        validateRolloutPercentage(rolloutPercentage);

        if (existsByNameAndEnvironment(name, environment)) {
            throw new IllegalArgumentException(
                    String.format("Feature flag with name '%s' already exists in environment '%s'", name, environment)
            );
        }

        FeatureFlag flag = new FeatureFlag(name, description, enabled, status, createdBy);
        flag.setEnvironment(environment);
        flag.setRolloutPercentage(rolloutPercentage);

        return featureFlagRepository.save(flag);
    }

    /**
     * Get feature flag by ID.
     */
    @Transactional(readOnly = true)
    public Optional<FeatureFlag> getFlagById(Long id) {
        logger.debug("Retrieving feature flag by ID: {}", id);
        return featureFlagRepository.findById(id);
    }

    /**
     * Get feature flag by name.
     */
    @Transactional(readOnly = true)
    public Optional<FeatureFlag> getFlagByName(String name) {
        logger.debug("Retrieving feature flag by name: {}", name);
        return featureFlagRepository.findByName(name);
    }

    /**
     * Get all feature flags.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getAllFlags() {
        logger.debug("Retrieving all feature flags");
        return featureFlagRepository.findAll();
    }

    /**
     * Get flags by environment.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getFlagsByEnvironment(String environment) {
        logger.debug("Retrieving feature flags for environment: {}", environment);
        return featureFlagRepository.findByEnvironment(environment);
    }

    /**
     * Get flags by status.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getFlagsByStatus(FeatureFlag.FlagStatus status) {
        logger.debug("Retrieving feature flags with status: {}", status);
        return featureFlagRepository.findByStatus(status);
    }

    /**
     * Get all enabled flags.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getEnabledFlags() {
        logger.debug("Retrieving all enabled feature flags");
        return featureFlagRepository.findByEnabledTrue();
    }

    /**
     * Get active flags by environment.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getActiveFlagsByEnvironment(String environment) {
        logger.debug("Retrieving active feature flags for environment: {}", environment);
        return featureFlagRepository.findByStatusAndEnvironment(FeatureFlag.FlagStatus.ACTIVE, environment);
    }

    /**
     * Update feature flag.
     */
    public FeatureFlag updateFeatureFlag(Long id, String name, String description, String environment) {
        logger.info("Updating feature flag ID: {}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));

        // Check for name conflicts (if name is being changed)
        if (!flag.getName().equals(name) && existsByNameAndEnvironment(name, environment)) {
            throw new IllegalArgumentException(
                    String.format("Feature flag with name '%s' already exists in environment '%s'", name, environment)
            );
        }

        flag.setName(name);
        flag.setDescription(description);
        flag.setEnvironment(environment);

        return featureFlagRepository.save(flag);
    }

    /**
     * Delete feature flag (soft delete by archiving).
     */
    public void deleteFeatureFlag(Long id) {
        logger.info("Archiving feature flag ID: {}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));

        flag.archive(); // Soft delete - sets status to ARCHIVED and disabled
        featureFlagRepository.save(flag);

        logger.info("Successfully archived feature flag: {}", flag.getName());
    }

    // ============================================
    // REAL-TIME TOGGLE OPERATIONS
    // ============================================

    /**
     * Toggle feature flag state (real-time operation).
     * This is the core functionality for instant feature toggling.
     */
    public FeatureFlag toggleFlag(Long id) {
        logger.info("Toggling feature flag ID: {}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));

        boolean wasEnabled = flag.getEnabled();
        flag.toggle();

        FeatureFlag savedFlag = featureFlagRepository.save(flag);

        logger.info("Successfully toggled feature flag '{}' from {} to {}",
                flag.getName(), wasEnabled, savedFlag.getEnabled());

        return savedFlag;
    }

    /**
     * Enable feature flag.
     */
    public FeatureFlag enableFlag(Long id) {
        logger.info("Enabling feature flag ID: {}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));

        flag.activate();
        FeatureFlag savedFlag = featureFlagRepository.save(flag);

        logger.info("Successfully enabled feature flag: {}", flag.getName());
        return savedFlag;
    }

    /**
     * Disable feature flag.
     */
    public FeatureFlag disableFlag(Long id) {
        logger.info("Disabling feature flag ID: {}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));

        flag.deactivate();
        FeatureFlag savedFlag = featureFlagRepository.save(flag);

        logger.info("Successfully disabled feature flag: {}", flag.getName());
        return savedFlag;
    }

    /**
     * Enable flag by name.
     */
    public FeatureFlag enableFlagByName(String name) {
        logger.info("Enabling feature flag by name: {}", name);

        FeatureFlag flag = featureFlagRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with name: " + name));

        flag.activate();
        return featureFlagRepository.save(flag);
    }

    /**
     * Disable flag by name.
     */
    public FeatureFlag disableFlagByName(String name) {
        logger.info("Disabling feature flag by name: {}", name);

        FeatureFlag flag = featureFlagRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with name: " + name));

        flag.deactivate();
        return featureFlagRepository.save(flag);
    }

    // ============================================
    // ROLLOUT PERCENTAGE MANAGEMENT
    // ============================================

    /**
     * Update rollout percentage for gradual feature releases.
     */
    public FeatureFlag updateRolloutPercentage(Long id, Integer percentage) {
        logger.info("Updating rollout percentage for flag ID: {} to {}%", id, percentage);

        validateRolloutPercentage(percentage);

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));

        flag.setRolloutPercentage(percentage);
        FeatureFlag savedFlag = featureFlagRepository.save(flag);

        logger.info("Successfully updated rollout percentage for '{}' to {}%", flag.getName(), percentage);
        return savedFlag;
    }

    /**
     * Gradually increase rollout percentage.
     */
    public FeatureFlag increaseRollout(Long id, Integer incrementPercentage) {
        logger.info("Increasing rollout for flag ID: {} by {}%", id, incrementPercentage);

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));

        int currentRollout = flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0;
        int newRollout = Math.min(100, currentRollout + incrementPercentage);

        flag.setRolloutPercentage(newRollout);
        return featureFlagRepository.save(flag);
    }

    /**
     * Get flags with rollout percentage greater than specified value.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getFlagsWithRolloutGreaterThan(Integer percentage) {
        return featureFlagRepository.findEnabledFlagsWithRolloutGreaterThan(percentage);
    }

    // ============================================
    // FLAG EVALUATION FOR USERS
    // ============================================

    /**
     * Evaluate if a flag should be enabled for a specific user.
     * Considers rollout percentage and user hash for consistent assignment.
     */
    public boolean isFlagEnabledForUser(String flagName, String userId, String environment) {
        logger.debug("Evaluating flag '{}' for user '{}' in environment '{}'", flagName, userId, environment);

        Optional<FeatureFlag> flagOpt = featureFlagRepository.findByName(flagName);
        if (flagOpt.isEmpty()) {
            logger.debug("Flag '{}' not found", flagName);
            return false;
        }

        FeatureFlag flag = flagOpt.get();

        // Check if flag is active and enabled
        if (!flag.isActive()) {
            logger.debug("Flag '{}' is not active", flagName);
            return false;
        }

        // Check environment match
        if (!flag.getEnvironment().equals(environment)) {
            logger.debug("Flag '{}' environment mismatch. Expected: {}, Got: {}",
                    flagName, flag.getEnvironment(), environment);
            return false;
        }

        // Check rollout percentage
        Integer rolloutPercentage = flag.getRolloutPercentage();
        if (rolloutPercentage == null || rolloutPercentage == 0) {
            logger.debug("Flag '{}' has 0% rollout", flagName);
            return false;
        }

        if (rolloutPercentage >= 100) {
            logger.debug("Flag '{}' has 100% rollout - enabled for all users", flagName);
            return true;
        }

        // Calculate user percentile based on user ID hash for consistent assignment
        int userPercentile = calculateUserPercentile(userId, flagName);
        boolean enabled = userPercentile <= rolloutPercentage;

        logger.debug("Flag '{}' for user '{}': percentile={}, rollout={}%, enabled={}",
                flagName, userId, userPercentile, rolloutPercentage, enabled);

        return enabled;
    }

    /**
     * Get all flags available for a user based on rollout percentage.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getAvailableFlagsForUser(String userId, String environment) {
        int userPercentile = calculateUserPercentile(userId, "global");
        return featureFlagRepository.findAvailableFlagsForUser(environment, userPercentile);
    }

    // ============================================
    // BULK OPERATIONS
    // ============================================

    /**
     * Enable multiple flags by IDs.
     */
    public List<FeatureFlag> enableFlags(List<Long> flagIds) {
        logger.info("Enabling {} feature flags", flagIds.size());

        return flagIds.stream()
                .map(this::enableFlag)
                .toList();
    }

    /**
     * Disable multiple flags by IDs.
     */
    public List<FeatureFlag> disableFlags(List<Long> flagIds) {
        logger.info("Disabling {} feature flags", flagIds.size());

        return flagIds.stream()
                .map(this::disableFlag)
                .toList();
    }

    /**
     * Update rollout percentage for multiple flags.
     */
    public List<FeatureFlag> updateRolloutForFlags(List<Long> flagIds, Integer percentage) {
        logger.info("Updating rollout to {}% for {} feature flags", percentage, flagIds.size());

        validateRolloutPercentage(percentage);

        return flagIds.stream()
                .map(id -> updateRolloutPercentage(id, percentage))
                .toList();
    }

    // ============================================
    // ANALYTICS AND REPORTING
    // ============================================

    /**
     * Get feature flag statistics by environment.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getFlagStatsByEnvironment() {
        logger.debug("Retrieving feature flag statistics by environment");
        return featureFlagRepository.getFeatureFlagStatsByEnvironment();
    }

    /**
     * Get recently updated flags.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getRecentlyUpdatedFlags(LocalDateTime since) {
        logger.debug("Retrieving flags updated since: {}", since);
        return featureFlagRepository.findRecentlyUpdated(since);
    }

    /**
     * Get flags created by specific user.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getFlagsByCreator(String createdBy) {
        logger.debug("Retrieving flags created by: {}", createdBy);
        return featureFlagRepository.findByCreatedByOrderByCreatedAtDesc(createdBy);
    }

    /**
     * Get flag counts by status.
     */
    @Transactional(readOnly = true)
    public long getCountByStatus(FeatureFlag.FlagStatus status) {
        return featureFlagRepository.countByStatus(status);
    }

    /**
     * Get enabled flag count.
     */
    @Transactional(readOnly = true)
    public long getEnabledFlagCount() {
        return featureFlagRepository.countByEnabledTrue();
    }

    // ============================================
    // VALIDATION METHODS
    // ============================================

    /**
     * Validate flag name.
     */
    private void validateFlagName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Flag name cannot be null or empty");
        }
        if (name.length() < 3 || name.length() > 100) {
            throw new IllegalArgumentException("Flag name must be between 3 and 100 characters");
        }
        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Flag name can only contain letters, numbers, underscores, and hyphens");
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

    /**
     * Validate rollout percentage.
     */
    private void validateRolloutPercentage(Integer percentage) {
        if (percentage == null) {
            throw new IllegalArgumentException("Rollout percentage cannot be null");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100");
        }
    }

    /**
     * Check if flag exists by name and environment.
     */
    private boolean existsByNameAndEnvironment(String name, String environment) {
        return featureFlagRepository.findByName(name)
                .map(flag -> flag.getEnvironment().equals(environment))
                .orElse(false);
    }

    /**
     * Calculate user percentile for consistent flag assignment.
     * Uses hash-based algorithm to ensure same user always gets same result.
     */
    private int calculateUserPercentile(String userId, String context) {
        String combined = userId + ":" + context;
        int hash = Math.abs(combined.hashCode());
        return hash % 100 + 1; // Return 1-100
    }

    // ============================================
    // EXISTENCE CHECKS
    // ============================================

    /**
     * Check if flag exists by name.
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return featureFlagRepository.existsByName(name);
    }

    /**
     * Check if flag exists by ID.
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return featureFlagRepository.existsById(id);
    }
}