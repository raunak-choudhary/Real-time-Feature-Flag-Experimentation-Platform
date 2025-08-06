package com.rex.service;

import com.rex.model.Experiment;
import com.rex.model.FeatureFlag;
import com.rex.model.Metrics;
import com.rex.repository.MetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for Metrics and Analytics operations.
 * Provides comprehensive event tracking, analytics calculation,
 * and real-time metrics aggregation for feature flags and experiments.
 *
 * Key Features:
 * - Event tracking for flags and experiments
 * - Real-time analytics and aggregation
 * - Performance metrics monitoring
 * - Dashboard data preparation
 * - Conversion tracking and funnel analysis
 */
@Service
@Transactional
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final MetricsRepository metricsRepository;

    @Autowired
    public MetricsService(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    // ============================================
    // EVENT TRACKING - FEATURE FLAGS
    // ============================================

    /**
     * Track feature flag exposure event.
     */
    public Metrics trackFlagExposure(String userId, FeatureFlag featureFlag, String sessionId,
                                     String environment, String userAgent, String pageUrl) {
        logger.debug("Tracking flag exposure: {} for user: {}", featureFlag.getName(), userId);

        Metrics metrics = new Metrics(userId, featureFlag, Metrics.EventType.FLAG_EXPOSURE);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setUserAgent(userAgent);
        metrics.setPageUrl(pageUrl);
        metrics.setEventName("flag_exposure_" + featureFlag.getName());

        return metricsRepository.save(metrics);
    }

    /**
     * Track feature flag toggle event.
     */
    public Metrics trackFlagToggle(String userId, FeatureFlag featureFlag, boolean newState,
                                   String environment, String triggeredBy) {
        logger.info("Tracking flag toggle: {} -> {} by user: {}",
                featureFlag.getName(), newState, triggeredBy);

        Metrics.EventType eventType = newState ? Metrics.EventType.FLAG_ENABLED : Metrics.EventType.FLAG_DISABLED;

        Metrics metrics = new Metrics(userId, featureFlag, eventType);
        metrics.setEnvironment(environment);
        metrics.setEventName("flag_toggle_" + featureFlag.getName());
        metrics.setEventValue(newState ? 1.0 : 0.0);
        metrics.setProperties(String.format("{\"triggered_by\":\"%s\",\"new_state\":%b}", triggeredBy, newState));

        return metricsRepository.save(metrics);
    }

    /**
     * Track feature flag usage with context.
     */
    public Metrics trackFlagUsage(String userId, FeatureFlag featureFlag, String context,
                                  String sessionId, String environment) {
        logger.debug("Tracking flag usage: {} in context: {}", featureFlag.getName(), context);

        Metrics metrics = new Metrics(userId, featureFlag, Metrics.EventType.FLAG_EXPOSURE);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setEventName("flag_usage_" + featureFlag.getName());
        metrics.setProperties(String.format("{\"context\":\"%s\"}", context));

        return metricsRepository.save(metrics);
    }

    // ============================================
    // EVENT TRACKING - EXPERIMENTS
    // ============================================

    /**
     * Track experiment exposure event.
     */
    public Metrics trackExperimentExposure(String userId, Experiment experiment, String variantName,
                                           String sessionId, String environment, String userAgent, String pageUrl) {
        logger.debug("Tracking experiment exposure: {} variant: {} for user: {}",
                experiment.getName(), variantName, userId);

        Metrics metrics = new Metrics(userId, experiment, Metrics.EventType.EXPERIMENT_EXPOSURE, variantName);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setUserAgent(userAgent);
        metrics.setPageUrl(pageUrl);
        metrics.setEventName("experiment_exposure_" + experiment.getName());

        return metricsRepository.save(metrics);
    }

    /**
     * Track experiment assignment event.
     */
    public Metrics trackExperimentAssignment(String userId, Experiment experiment, String variantName,
                                             String assignmentMethod, String environment) {
        logger.info("Tracking experiment assignment: {} variant: {} for user: {} via: {}",
                experiment.getName(), variantName, userId, assignmentMethod);

        Metrics metrics = new Metrics(userId, experiment, Metrics.EventType.EXPERIMENT_ASSIGNMENT, variantName);
        metrics.setEnvironment(environment);
        metrics.setEventName("experiment_assignment_" + experiment.getName());
        metrics.setProperties(String.format("{\"assignment_method\":\"%s\"}", assignmentMethod));

        return metricsRepository.save(metrics);
    }

    /**
     * Track conversion event for experiment.
     */
    public Metrics trackConversion(String userId, Experiment experiment, String variantName,
                                   Double conversionValue, String sessionId, String environment) {
        logger.info("Tracking conversion: {} variant: {} value: {} for user: {}",
                experiment.getName(), variantName, conversionValue, userId);

        Metrics metrics = new Metrics(userId, experiment, Metrics.EventType.CONVERSION,
                variantName, conversionValue);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setEventName("conversion_" + experiment.getName());

        return metricsRepository.save(metrics);
    }

    /**
     * Track purchase conversion with revenue.
     */
    public Metrics trackPurchase(String userId, Experiment experiment, String variantName,
                                 Double revenue, String sessionId, String environment) {
        logger.info("Tracking purchase: {} variant: {} revenue: {} for user: {}",
                experiment.getName(), variantName, revenue, userId);

        Metrics metrics = new Metrics(userId, experiment, Metrics.EventType.PURCHASE, variantName);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setRevenue(revenue);
        metrics.setConversionValue(revenue);
        metrics.setEventName("purchase_" + experiment.getName());

        return metricsRepository.save(metrics);
    }

    // ============================================
    // GENERAL EVENT TRACKING
    // ============================================

    /**
     * Track page view event.
     */
    public Metrics trackPageView(String userId, String pageUrl, String sessionId,
                                 String environment, String userAgent, String referrerUrl) {
        logger.debug("Tracking page view: {} for user: {}", pageUrl, userId);

        Metrics metrics = new Metrics(userId, Metrics.EventType.PAGE_VIEW, "page_view", null);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setUserAgent(userAgent);
        metrics.setPageUrl(pageUrl);
        metrics.setReferrerUrl(referrerUrl);

        return metricsRepository.save(metrics);
    }

    /**
     * Track click event.
     */
    public Metrics trackClick(String userId, String element, String pageUrl, String sessionId,
                              String environment, Experiment experiment, String variantName) {
        logger.debug("Tracking click: {} on {} for user: {}", element, pageUrl, userId);

        Metrics metrics = new Metrics(userId, Metrics.EventType.CLICK, element, 1.0);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setPageUrl(pageUrl);
        metrics.setExperiment(experiment);
        metrics.setVariantName(variantName);

        return metricsRepository.save(metrics);
    }

    /**
     * Track error event.
     */
    public Metrics trackError(String userId, String errorMessage, String pageUrl, String sessionId,
                              String environment, FeatureFlag relatedFlag) {
        logger.warn("Tracking error: {} on {} for user: {}", errorMessage, pageUrl, userId);

        Metrics metrics = new Metrics(userId, Metrics.EventType.ERROR, "application_error", null);
        metrics.setSessionId(sessionId);
        metrics.setEnvironment(environment);
        metrics.setPageUrl(pageUrl);
        metrics.setErrorMessage(errorMessage);
        metrics.setFeatureFlag(relatedFlag);

        return metricsRepository.save(metrics);
    }

    /**
     * Track performance metric.
     */
    public Metrics trackPerformance(String userId, String metricName, Double value, Long durationMs,
                                    String environment, String pageUrl) {
        logger.debug("Tracking performance: {} = {} ({}ms) for user: {}", metricName, value, durationMs, userId);

        Metrics.EventType eventType = metricName.contains("load") ?
                Metrics.EventType.LOAD_TIME : Metrics.EventType.API_RESPONSE_TIME;

        Metrics metrics = new Metrics(userId, eventType, metricName, value);
        metrics.setEnvironment(environment);
        metrics.setPageUrl(pageUrl);
        metrics.setDurationMs(durationMs);

        return metricsRepository.save(metrics);
    }

    // ============================================
    // ANALYTICS & AGGREGATION
    // ============================================

    /**
     * Get experiment performance metrics.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getExperimentPerformance(Long experimentId) {
        logger.debug("Calculating experiment performance for ID: {}", experimentId);
        return metricsRepository.getExperimentPerformance(experimentId);
    }

    /**
     * Get experiment performance summary.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExperimentPerformanceSummary(Long experimentId) {
        logger.debug("Calculating experiment performance summary for ID: {}", experimentId);

        List<Object[]> rawData = metricsRepository.getExperimentPerformance(experimentId);
        Map<String, Object> summary = new HashMap<>();

        for (Object[] row : rawData) {
            String variantName = (String) row[0];
            Long totalEvents = (Long) row[1];
            Long conversions = (Long) row[2];
            Long uniqueUsers = (Long) row[3];
            Double avgEventValue = (Double) row[4];
            Double totalRevenue = (Double) row[5];

            double conversionRate = totalEvents > 0 ? (conversions.doubleValue() / totalEvents.doubleValue()) * 100 : 0;

            Map<String, Object> variantData = new HashMap<>();
            variantData.put("totalEvents", totalEvents);
            variantData.put("conversions", conversions);
            variantData.put("uniqueUsers", uniqueUsers);
            variantData.put("conversionRate", conversionRate);
            variantData.put("avgEventValue", avgEventValue);
            variantData.put("totalRevenue", totalRevenue);

            summary.put(variantName, variantData);
        }

        return summary;
    }

    /**
     * Get feature flag usage statistics.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getFeatureFlagUsage(Long flagId) {
        logger.debug("Calculating feature flag usage for ID: {}", flagId);
        return metricsRepository.getFeatureFlagUsage(flagId);
    }

    /**
     * Get feature flag usage summary.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeatureFlagUsageSummary(Long flagId) {
        logger.debug("Calculating feature flag usage summary for ID: {}", flagId);

        List<Object[]> rawData = metricsRepository.getFeatureFlagUsage(flagId);

        if (rawData.isEmpty()) {
            return Map.of("totalExposures", 0L, "uniqueUsers", 0L,
                    "enabledEvents", 0L, "disabledEvents", 0L);
        }

        Object[] data = rawData.get(0);
        return Map.of(
                "flagId", data[0],
                "totalExposures", data[1],
                "uniqueUsers", data[2],
                "enabledEvents", data[3],
                "disabledEvents", data[4]
        );
    }

    /**
     * Get conversion funnel for experiment.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getConversionFunnel(Long experimentId) {
        logger.debug("Calculating conversion funnel for experiment ID: {}", experimentId);
        return metricsRepository.getConversionFunnel(experimentId);
    }

    /**
     * Get conversion funnel summary.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> getConversionFunnelSummary(Long experimentId) {
        logger.debug("Calculating conversion funnel summary for experiment ID: {}", experimentId);

        List<Object[]> rawData = metricsRepository.getConversionFunnel(experimentId);
        Map<String, Map<String, Long>> funnelData = new HashMap<>();

        for (Object[] row : rawData) {
            String eventType = ((Metrics.EventType) row[0]).toString();
            String variantName = (String) row[1];
            Long eventCount = (Long) row[2];

            funnelData.computeIfAbsent(variantName, k -> new HashMap<>())
                    .put(eventType, eventCount);
        }

        return funnelData;
    }

    // ============================================
    // DASHBOARD METRICS
    // ============================================

    /**
     * Get hourly metrics for dashboard.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getHourlyMetrics(LocalDateTime startDate, String environment) {
        logger.debug("Retrieving hourly metrics since: {} for environment: {}", startDate, environment);
        return metricsRepository.getHourlyMetrics(startDate, environment);
    }

    /**
     * Get daily metrics aggregation.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDailyMetrics(LocalDateTime startDate) {
        logger.debug("Retrieving daily metrics since: {}", startDate);
        return metricsRepository.getDailyMetrics(startDate);
    }

    /**
     * Get top performing variants.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTopPerformingVariants(Long minExposures) {
        logger.debug("Retrieving top performing variants with min exposures: {}", minExposures);
        return metricsRepository.getTopPerformingVariants(minExposures);
    }

    /**
     * Get error metrics for monitoring.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getErrorMetrics(LocalDateTime sinceDate) {
        logger.debug("Retrieving error metrics since: {}", sinceDate);
        return metricsRepository.getErrorMetrics(sinceDate);
    }

    /**
     * Get user engagement metrics.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserEngagement(LocalDateTime sinceDate, Long minEvents) {
        logger.debug("Retrieving user engagement since: {} with min events: {}", sinceDate, minEvents);
        return metricsRepository.getUserEngagement(sinceDate, minEvents);
    }

    /**
     * Get platform distribution.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getPlatformDistribution(LocalDateTime sinceDate) {
        logger.debug("Retrieving platform distribution since: {}", sinceDate);
        return metricsRepository.getPlatformDistribution(sinceDate);
    }

    /**
     * Get revenue metrics by experiment.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRevenueMetrics() {
        logger.debug("Retrieving revenue metrics by experiment");
        return metricsRepository.getRevenueMetrics();
    }

    // ============================================
    // REAL-TIME ANALYTICS
    // ============================================

    /**
     * Get real-time experiment stats.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRealTimeExperimentStats(Long experimentId) {
        logger.debug("Calculating real-time stats for experiment ID: {}", experimentId);

        Map<String, Object> stats = new HashMap<>();

        // Get basic counts
        long totalExposures = metricsRepository.countByExperiment_Id(experimentId);
        stats.put("totalExposures", totalExposures);

        // Get conversion data
        List<Object[]> performanceData = getExperimentPerformance(experimentId);
        stats.put("variants", performanceData);

        // Get recent activity (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Object[]> recentActivity = metricsRepository.getHourlyMetrics(yesterday, "production");
        stats.put("recentActivity", recentActivity);

        return stats;
    }

    /**
     * Get real-time flag stats.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRealTimeFlagStats(Long flagId) {
        logger.debug("Calculating real-time stats for flag ID: {}", flagId);

        Map<String, Object> stats = new HashMap<>();

        // Get basic counts
        long totalExposures = metricsRepository.countByFeatureFlag_Id(flagId);
        stats.put("totalExposures", totalExposures);

        // Get usage data
        Map<String, Object> usageData = getFeatureFlagUsageSummary(flagId);
        stats.put("usage", usageData);

        // Get recent errors
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Object[]> errorData = getErrorMetrics(yesterday);
        stats.put("recentErrors", errorData);

        return stats;
    }

    /**
     * Get dashboard overview metrics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardOverview(String environment) {
        logger.debug("Calculating dashboard overview for environment: {}", environment);

        Map<String, Object> overview = new HashMap<>();
        LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
        LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);

        // Total events in last 24 hours
        List<Object[]> hourlyData = getHourlyMetrics(last24Hours, environment);
        long totalEvents24h = hourlyData.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();
        overview.put("totalEvents24h", totalEvents24h);

        // Unique users in last 24 hours
        long uniqueUsers24h = hourlyData.stream()
                .mapToLong(row -> (Long) row[2])
                .sum();
        overview.put("uniqueUsers24h", uniqueUsers24h);

        // Error count in last 24 hours
        List<Object[]> errorData = getErrorMetrics(last24Hours);
        long errorCount24h = errorData.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();
        overview.put("errorCount24h", errorCount24h);

        // Top performing variants
        List<Object[]> topVariants = getTopPerformingVariants(100L);
        overview.put("topPerformingVariants", topVariants);

        // Platform distribution
        List<Object[]> platformData = getPlatformDistribution(lastWeek);
        overview.put("platformDistribution", platformData);

        return overview;
    }

    // ============================================
    // QUERY METHODS
    // ============================================

    /**
     * Get metrics by user ID.
     */
    @Transactional(readOnly = true)
    public List<Metrics> getMetricsByUserId(String userId) {
        return metricsRepository.findByUserId(userId);
    }

    /**
     * Get metrics by event type.
     */
    @Transactional(readOnly = true)
    public List<Metrics> getMetricsByEventType(Metrics.EventType eventType) {
        return metricsRepository.findByEventType(eventType);
    }

    /**
     * Get metrics by date range.
     */
    @Transactional(readOnly = true)
    public List<Metrics> getMetricsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return metricsRepository.findByTimestampBetween(startDate, endDate);
    }

    /**
     * Get metrics by experiment.
     */
    @Transactional(readOnly = true)
    public List<Metrics> getMetricsByExperiment(Long experimentId) {
        return metricsRepository.findByExperiment_Id(experimentId);
    }

    /**
     * Get metrics by feature flag.
     */
    @Transactional(readOnly = true)
    public List<Metrics> getMetricsByFeatureFlag(Long flagId) {
        return metricsRepository.findByFeatureFlag_Id(flagId);
    }

    /**
     * Get high-value events.
     */
    @Transactional(readOnly = true)
    public List<Metrics> getHighValueEvents(Double minValue, LocalDateTime sinceDate) {
        return metricsRepository.findHighValueEvents(minValue, sinceDate);
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Count metrics by event type.
     */
    @Transactional(readOnly = true)
    public long countByEventType(Metrics.EventType eventType) {
        return metricsRepository.countByEventType(eventType);
    }

    /**
     * Count metrics by experiment.
     */
    @Transactional(readOnly = true)
    public long countByExperiment(Long experimentId) {
        return metricsRepository.countByExperiment_Id(experimentId);
    }

    /**
     * Count metrics by feature flag.
     */
    @Transactional(readOnly = true)
    public long countByFeatureFlag(Long flagId) {
        return metricsRepository.countByFeatureFlag_Id(flagId);
    }

    /**
     * Get metrics by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Metrics> getMetricsById(Long id) {
        return metricsRepository.findById(id);
    }

    /**
     * Get all metrics.
     */
    @Transactional(readOnly = true)
    public List<Metrics> getAllMetrics() {
        return metricsRepository.findAll();
    }

    // ============================================
    // BULK OPERATIONS
    // ============================================

    /**
     * Save multiple metrics in batch.
     */
    public List<Metrics> saveMetricsBatch(List<Metrics> metricsList) {
        logger.info("Saving metrics batch of size: {}", metricsList.size());
        return metricsRepository.saveAll(metricsList);
    }

    /**
     * Delete metrics older than specified date.
     */
    public void cleanupOldMetrics(LocalDateTime cutoffDate) {
        logger.info("Cleaning up metrics older than: {}", cutoffDate);

        List<Metrics> oldMetrics = metricsRepository.findByTimestampBetween(
                LocalDateTime.of(2020, 1, 1, 0, 0), cutoffDate);

        if (!oldMetrics.isEmpty()) {
            metricsRepository.deleteAll(oldMetrics);
            logger.info("Deleted {} old metrics records", oldMetrics.size());
        }
    }
}