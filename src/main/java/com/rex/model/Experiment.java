package com.rex.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "experiments")
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Experiment name cannot be blank")
    @Size(min = 3, max = 100, message = "Experiment name must be between 3 and 100 characters")
    private String name;

    @Column(length = 1000)
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperimentStatus status = ExperimentStatus.DRAFT;

    @Column(name = "traffic_percentage", nullable = false)
    @Min(value = 1, message = "Traffic percentage must be at least 1%")
    @Max(value = 100, message = "Traffic percentage cannot exceed 100%")
    private Integer trafficPercentage = 50;

    @Column(name = "control_variant_name", nullable = false)
    @NotBlank(message = "Control variant name cannot be blank")
    private String controlVariantName = "control";

    @Column(name = "test_variant_name", nullable = false)
    @NotBlank(message = "Test variant name cannot be blank")
    private String testVariantName = "test";

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_by")
    @Size(max = 100, message = "Created by field cannot exceed 100 characters")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "environment")
    private String environment = "development";

    @Column(name = "hypothesis", length = 2000)
    @Size(max = 2000, message = "Hypothesis cannot exceed 2000 characters")
    private String hypothesis;

    @Column(name = "success_metric")
    private String successMetric;

    @Column(name = "expected_improvement")
    private Double expectedImprovement;

    @Column(name = "confidence_level")
    private Double confidenceLevel = 95.0;

    @Column(name = "minimum_sample_size")
    private Integer minimumSampleSize;

    @Column(name = "current_sample_size")
    private Integer currentSampleSize = 0;

    // Enum for Experiment Status
    public enum ExperimentStatus {
        DRAFT,
        READY,
        RUNNING,
        PAUSED,
        COMPLETED,
        ARCHIVED,
        CANCELLED
    }

    // Default constructor
    public Experiment() {}

    // Constructor with essential fields
    public Experiment(String name, String description, String createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.status = ExperimentStatus.DRAFT;
    }

    // Constructor with main fields
    public Experiment(String name, String description, Integer trafficPercentage,
                      String controlVariantName, String testVariantName, String createdBy) {
        this.name = name;
        this.description = description;
        this.trafficPercentage = trafficPercentage;
        this.controlVariantName = controlVariantName;
        this.testVariantName = testVariantName;
        this.createdBy = createdBy;
        this.status = ExperimentStatus.DRAFT;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public Integer getTrafficPercentage() {
        return trafficPercentage;
    }

    public void setTrafficPercentage(Integer trafficPercentage) {
        this.trafficPercentage = trafficPercentage;
    }

    public String getControlVariantName() {
        return controlVariantName;
    }

    public void setControlVariantName(String controlVariantName) {
        this.controlVariantName = controlVariantName;
    }

    public String getTestVariantName() {
        return testVariantName;
    }

    public void setTestVariantName(String testVariantName) {
        this.testVariantName = testVariantName;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getHypothesis() {
        return hypothesis;
    }

    public void setHypothesis(String hypothesis) {
        this.hypothesis = hypothesis;
    }

    public String getSuccessMetric() {
        return successMetric;
    }

    public void setSuccessMetric(String successMetric) {
        this.successMetric = successMetric;
    }

    public Double getExpectedImprovement() {
        return expectedImprovement;
    }

    public void setExpectedImprovement(Double expectedImprovement) {
        this.expectedImprovement = expectedImprovement;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public Integer getMinimumSampleSize() {
        return minimumSampleSize;
    }

    public void setMinimumSampleSize(Integer minimumSampleSize) {
        this.minimumSampleSize = minimumSampleSize;
    }

    public Integer getCurrentSampleSize() {
        return currentSampleSize;
    }

    public void setCurrentSampleSize(Integer currentSampleSize) {
        this.currentSampleSize = currentSampleSize;
    }

    // Business logic methods
    public void start() {
        if (this.status == ExperimentStatus.READY || this.status == ExperimentStatus.PAUSED) {
            this.status = ExperimentStatus.RUNNING;
            if (this.startDate == null) {
                this.startDate = LocalDateTime.now();
            }
        }
    }

    public void pause() {
        if (this.status == ExperimentStatus.RUNNING) {
            this.status = ExperimentStatus.PAUSED;
        }
    }

    public void stop() {
        if (this.status == ExperimentStatus.RUNNING || this.status == ExperimentStatus.PAUSED) {
            this.status = ExperimentStatus.COMPLETED;
            this.endDate = LocalDateTime.now();
        }
    }

    public void markReady() {
        if (this.status == ExperimentStatus.DRAFT) {
            this.status = ExperimentStatus.READY;
        }
    }

    public void archive() {
        if (this.status == ExperimentStatus.COMPLETED || this.status == ExperimentStatus.CANCELLED) {
            this.status = ExperimentStatus.ARCHIVED;
        }
    }

    public void cancel() {
        if (this.status != ExperimentStatus.COMPLETED && this.status != ExperimentStatus.ARCHIVED) {
            this.status = ExperimentStatus.CANCELLED;
            this.endDate = LocalDateTime.now();
        }
    }

    public boolean isRunning() {
        return this.status == ExperimentStatus.RUNNING;
    }

    public boolean isActive() {
        return this.status == ExperimentStatus.RUNNING || this.status == ExperimentStatus.PAUSED;
    }

    public boolean canBeStarted() {
        return this.status == ExperimentStatus.READY || this.status == ExperimentStatus.PAUSED;
    }

    public void incrementSampleSize() {
        this.currentSampleSize++;
    }

    public Double getCompletionPercentage() {
        if (minimumSampleSize == null || minimumSampleSize == 0) {
            return 0.0;
        }
        return Math.min(100.0, (currentSampleSize.doubleValue() / minimumSampleSize.doubleValue()) * 100.0);
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Experiment that = (Experiment) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    // toString
    @Override
    public String toString() {
        return "Experiment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", trafficPercentage=" + trafficPercentage +
                ", controlVariantName='" + controlVariantName + '\'' +
                ", testVariantName='" + testVariantName + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", currentSampleSize=" + currentSampleSize +
                ", environment='" + environment + '\'' +
                '}';
    }
}