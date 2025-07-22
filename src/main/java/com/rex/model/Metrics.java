package com.rex.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "metrics",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_feature_flag_id", columnList = "feature_flag_id"),
                @Index(name = "idx_experiment_id", columnList = "experiment_id"),
                @Index(name = "idx_event_type", columnList = "event_type"),
                @Index(name = "idx_timestamp", columnList = "timestamp"),
                @Index(name = "idx_user_experiment", columnList = "user_id, experiment_id"),
                @Index(name = "idx_user_flag", columnList = "user_id, feature_flag_id")
        })
public class Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID cannot be blank")
    @Size(max = 255, message = "User ID cannot exceed 255 characters")
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id")
    private FeatureFlag featureFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    @NotNull(message = "Event type cannot be null")
    private EventType eventType;

    @Column(name = "event_name")
    @Size(max = 100, message = "Event name cannot exceed 100 characters")
    private String eventName;

    @Column(name = "event_value")
    private Double eventValue;

    @Column(name = "variant_name")
    @Size(max = 100, message = "Variant name cannot exceed 100 characters")
    private String variantName;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "session_id")
    @Size(max = 255, message = "Session ID cannot exceed 255 characters")
    private String sessionId;

    @Column(name = "user_agent", length = 500)
    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    private String userAgent;

    @Column(name = "ip_address")
    @Size(max = 45, message = "IP address cannot exceed 45 characters") // IPv6 support
    private String ipAddress;

    @Column(name = "page_url", length = 1000)
    @Size(max = 1000, message = "Page URL cannot exceed 1000 characters")
    private String pageUrl;

    @Column(name = "referrer_url", length = 1000)
    @Size(max = 1000, message = "Referrer URL cannot exceed 1000 characters")
    private String referrerUrl;

    @Column(name = "environment")
    private String environment = "development";

    @Column(name = "properties", length = 2000)
    private String properties; // JSON string for custom properties

    @Column(name = "conversion_value")
    private Double conversionValue;

    @Column(name = "revenue")
    private Double revenue;

    @Column(name = "count_value")
    private Integer countValue = 1;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "device_type")
    @Size(max = 50, message = "Device type cannot exceed 50 characters")
    private String deviceType;

    @Column(name = "platform")
    @Size(max = 50, message = "Platform cannot exceed 50 characters")
    private String platform;

    // Enum for Event Type
    public enum EventType {
        // Feature Flag Events
        FLAG_EXPOSURE,
        FLAG_ENABLED,
        FLAG_DISABLED,
        FLAG_TOGGLED,

        // Experiment Events
        EXPERIMENT_EXPOSURE,
        EXPERIMENT_ASSIGNMENT,

        // User Interaction Events
        PAGE_VIEW,
        CLICK,
        IMPRESSION,

        // Conversion Events
        CONVERSION,
        PURCHASE,
        SIGNUP,
        LOGIN,

        // Performance Events
        LOAD_TIME,
        API_RESPONSE_TIME,

        // Error Events
        ERROR,
        EXCEPTION,

        // Custom Events
        CUSTOM
    }

    // Default constructor
    public Metrics() {}

    // Constructor for feature flag events
    public Metrics(String userId, FeatureFlag featureFlag, EventType eventType) {
        this.userId = userId;
        this.featureFlag = featureFlag;
        this.eventType = eventType;
    }

    // Constructor for experiment events
    public Metrics(String userId, Experiment experiment, EventType eventType, String variantName) {
        this.userId = userId;
        this.experiment = experiment;
        this.eventType = eventType;
        this.variantName = variantName;
    }

    // Constructor with event value
    public Metrics(String userId, EventType eventType, String eventName, Double eventValue) {
        this.userId = userId;
        this.eventType = eventType;
        this.eventName = eventName;
        this.eventValue = eventValue;
    }

    // Constructor for conversion events
    public Metrics(String userId, Experiment experiment, EventType eventType,
                   String variantName, Double conversionValue) {
        this.userId = userId;
        this.experiment = experiment;
        this.eventType = eventType;
        this.variantName = variantName;
        this.conversionValue = conversionValue;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public FeatureFlag getFeatureFlag() {
        return featureFlag;
    }

    public void setFeatureFlag(FeatureFlag featureFlag) {
        this.featureFlag = featureFlag;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Double getEventValue() {
        return eventValue;
    }

    public void setEventValue(Double eventValue) {
        this.eventValue = eventValue;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getReferrerUrl() {
        return referrerUrl;
    }

    public void setReferrerUrl(String referrerUrl) {
        this.referrerUrl = referrerUrl;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public Double getConversionValue() {
        return conversionValue;
    }

    public void setConversionValue(Double conversionValue) {
        this.conversionValue = conversionValue;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue;
    }

    public Integer getCountValue() {
        return countValue;
    }

    public void setCountValue(Integer countValue) {
        this.countValue = countValue;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    // Business logic methods
    public boolean isConversionEvent() {
        return eventType == EventType.CONVERSION ||
                eventType == EventType.PURCHASE ||
                eventType == EventType.SIGNUP;
    }

    public boolean isErrorEvent() {
        return eventType == EventType.ERROR ||
                eventType == EventType.EXCEPTION;
    }

    public boolean isFlagEvent() {
        return eventType == EventType.FLAG_EXPOSURE ||
                eventType == EventType.FLAG_ENABLED ||
                eventType == EventType.FLAG_DISABLED ||
                eventType == EventType.FLAG_TOGGLED;
    }

    public boolean isExperimentEvent() {
        return eventType == EventType.EXPERIMENT_EXPOSURE ||
                eventType == EventType.EXPERIMENT_ASSIGNMENT;
    }

    public Long getFeatureFlagId() {
        return featureFlag != null ? featureFlag.getId() : null;
    }

    public Long getExperimentId() {
        return experiment != null ? experiment.getId() : null;
    }

    public String getFeatureFlagName() {
        return featureFlag != null ? featureFlag.getName() : null;
    }

    public String getExperimentName() {
        return experiment != null ? experiment.getName() : null;
    }

    // Helper method to set context from request
    public void setWebContext(String userAgent, String ipAddress, String pageUrl, String referrerUrl) {
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.pageUrl = pageUrl;
        this.referrerUrl = referrerUrl;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metrics metrics = (Metrics) o;
        return Objects.equals(id, metrics.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // toString
    @Override
    public String toString() {
        return "Metrics{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", featureFlagId=" + getFeatureFlagId() +
                ", experimentId=" + getExperimentId() +
                ", eventType=" + eventType +
                ", eventName='" + eventName + '\'' +
                ", eventValue=" + eventValue +
                ", variantName='" + variantName + '\'' +
                ", timestamp=" + timestamp +
                ", conversionValue=" + conversionValue +
                ", environment='" + environment + '\'' +
                '}';
    }
}