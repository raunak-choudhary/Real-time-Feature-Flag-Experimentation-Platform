package com.rex.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_cohorts",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_experiment_id", columnList = "experiment_id"),
                @Index(name = "idx_user_experiment", columnList = "user_id, experiment_id")
        })
public class UserCohort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID cannot be blank")
    @Size(max = 255, message = "User ID cannot exceed 255 characters")
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    @NotNull(message = "Experiment cannot be null")
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "cohort_type", nullable = false)
    private CohortType cohortType;

    @Column(name = "variant_name", nullable = false)
    @NotBlank(message = "Variant name cannot be blank")
    private String variantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_method", nullable = false)
    private AssignmentMethod assignmentMethod = AssignmentMethod.HASH_BASED;

    @Column(name = "assignment_hash")
    private Integer assignmentHash;

    @Column(name = "session_id")
    @Size(max = 255, message = "Session ID cannot exceed 255 characters")
    private String sessionId;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "first_exposure_at")
    private LocalDateTime firstExposureAt;

    @Column(name = "last_exposure_at")
    private LocalDateTime lastExposureAt;

    @Column(name = "exposure_count")
    private Integer exposureCount = 0;

    @Column(name = "user_attributes", length = 1000)
    private String userAttributes; // JSON string for storing user attributes

    @Column(name = "environment")
    private String environment = "development";

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Enum for Cohort Type
    public enum CohortType {
        CONTROL,
        TREATMENT,
        EXCLUDED
    }

    // Enum for Assignment Method
    public enum AssignmentMethod {
        HASH_BASED,
        RANDOM,
        MANUAL,
        ATTRIBUTE_BASED,
        PERCENTAGE_BASED
    }

    // Default constructor
    public UserCohort() {}

    // Constructor with essential fields
    public UserCohort(String userId, Experiment experiment, CohortType cohortType, String variantName) {
        this.userId = userId;
        this.experiment = experiment;
        this.cohortType = cohortType;
        this.variantName = variantName;
        this.assignmentMethod = AssignmentMethod.HASH_BASED;
        this.isActive = true;
    }

    // Constructor with assignment method
    public UserCohort(String userId, Experiment experiment, CohortType cohortType,
                      String variantName, AssignmentMethod assignmentMethod) {
        this.userId = userId;
        this.experiment = experiment;
        this.cohortType = cohortType;
        this.variantName = variantName;
        this.assignmentMethod = assignmentMethod;
        this.isActive = true;
    }

    // Constructor with session tracking
    public UserCohort(String userId, String sessionId, Experiment experiment,
                      CohortType cohortType, String variantName) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.experiment = experiment;
        this.cohortType = cohortType;
        this.variantName = variantName;
        this.assignmentMethod = AssignmentMethod.HASH_BASED;
        this.isActive = true;
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

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public CohortType getCohortType() {
        return cohortType;
    }

    public void setCohortType(CohortType cohortType) {
        this.cohortType = cohortType;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public AssignmentMethod getAssignmentMethod() {
        return assignmentMethod;
    }

    public void setAssignmentMethod(AssignmentMethod assignmentMethod) {
        this.assignmentMethod = assignmentMethod;
    }

    public Integer getAssignmentHash() {
        return assignmentHash;
    }

    public void setAssignmentHash(Integer assignmentHash) {
        this.assignmentHash = assignmentHash;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getFirstExposureAt() {
        return firstExposureAt;
    }

    public void setFirstExposureAt(LocalDateTime firstExposureAt) {
        this.firstExposureAt = firstExposureAt;
    }

    public LocalDateTime getLastExposureAt() {
        return lastExposureAt;
    }

    public void setLastExposureAt(LocalDateTime lastExposureAt) {
        this.lastExposureAt = lastExposureAt;
    }

    public Integer getExposureCount() {
        return exposureCount;
    }

    public void setExposureCount(Integer exposureCount) {
        this.exposureCount = exposureCount;
    }

    public String getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(String userAttributes) {
        this.userAttributes = userAttributes;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // Business logic methods
    public void recordExposure() {
        LocalDateTime now = LocalDateTime.now();

        if (this.firstExposureAt == null) {
            this.firstExposureAt = now;
        }

        this.lastExposureAt = now;
        this.exposureCount++;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void reactivate() {
        this.isActive = true;
    }

    public boolean isControlGroup() {
        return this.cohortType == CohortType.CONTROL;
    }

    public boolean isTreatmentGroup() {
        return this.cohortType == CohortType.TREATMENT;
    }

    public boolean isExcluded() {
        return this.cohortType == CohortType.EXCLUDED;
    }

    public boolean hasBeenExposed() {
        return this.exposureCount > 0;
    }

    public Long getExperimentId() {
        return this.experiment != null ? this.experiment.getId() : null;
    }

    public String getExperimentName() {
        return this.experiment != null ? this.experiment.getName() : null;
    }

    // Helper method to calculate days since assignment
    public long getDaysSinceAssignment() {
        if (assignedAt == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(assignedAt.toLocalDate(), LocalDateTime.now().toLocalDate());
    }

    // Helper method to calculate days since first exposure
    public long getDaysSinceFirstExposure() {
        if (firstExposureAt == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(firstExposureAt.toLocalDate(), LocalDateTime.now().toLocalDate());
    }

    // equals and hashCode (based on userId and experiment)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserCohort that = (UserCohort) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(experiment, that.experiment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, experiment);
    }

    // toString
    @Override
    public String toString() {
        return "UserCohort{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", experimentId=" + getExperimentId() +
                ", experimentName='" + getExperimentName() + '\'' +
                ", cohortType=" + cohortType +
                ", variantName='" + variantName + '\'' +
                ", assignmentMethod=" + assignmentMethod +
                ", assignedAt=" + assignedAt +
                ", exposureCount=" + exposureCount +
                ", isActive=" + isActive +
                ", environment='" + environment + '\'' +
                '}';
    }
}