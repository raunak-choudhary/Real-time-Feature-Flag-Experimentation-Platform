package com.rex.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Flag name cannot be blank")
    @Size(min = 3, max = 100, message = "Flag name must be between 3 and 100 characters")
    private String name;

    @Column(length = 500)
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagStatus status = FlagStatus.INACTIVE;

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

    @Column(name = "rollout_percentage")
    private Integer rolloutPercentage = 0;

    // Enum for Flag Status
    public enum FlagStatus {
        ACTIVE,
        INACTIVE,
        ARCHIVED,
        DEPRECATED
    }

    // Default constructor
    public FeatureFlag() {}

    // Constructor with essential fields
    public FeatureFlag(String name, String description, String createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.enabled = false;
        this.status = FlagStatus.INACTIVE;
    }

    // Constructor with all main fields
    public FeatureFlag(String name, String description, Boolean enabled, FlagStatus status, String createdBy) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.status = status;
        this.createdBy = createdBy;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public FlagStatus getStatus() {
        return status;
    }

    public void setStatus(FlagStatus status) {
        this.status = status;
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

    public Integer getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(Integer rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    // Business logic methods
    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled && this.status == FlagStatus.INACTIVE) {
            this.status = FlagStatus.ACTIVE;
        } else if (!this.enabled && this.status == FlagStatus.ACTIVE) {
            this.status = FlagStatus.INACTIVE;
        }
    }

    public void activate() {
        this.enabled = true;
        this.status = FlagStatus.ACTIVE;
    }

    public void deactivate() {
        this.enabled = false;
        this.status = FlagStatus.INACTIVE;
    }

    public void archive() {
        this.enabled = false;
        this.status = FlagStatus.ARCHIVED;
    }

    public boolean isActive() {
        return this.enabled && this.status == FlagStatus.ACTIVE;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlag that = (FeatureFlag) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    // toString
    @Override
    public String toString() {
        return "FeatureFlag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", status=" + status +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", environment='" + environment + '\'' +
                ", rolloutPercentage=" + rolloutPercentage +
                '}';
    }
}