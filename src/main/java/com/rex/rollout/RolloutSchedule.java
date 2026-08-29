package com.rex.rollout;

import com.rex.model.FeatureFlag;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A staged rollout for one flag.
 *
 * <p>Stage position is persisted rather than held in memory, so a restart mid rollout resumes at
 * the correct stage instead of beginning again. The last safe percentage is recorded separately
 * because a rollback needs somewhere to return to, and the previous stage is the only percentage
 * known to have run without breaching a guardrail.
 */
@Entity
@Table(name = "rollout_schedules")
public class RolloutSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feature_flag_id", nullable = false)
  private FeatureFlag featureFlag;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RolloutStatus status = RolloutStatus.PENDING;

  @OneToMany(
      mappedBy = "schedule",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("stageOrder ASC")
  private List<RolloutStage> stages = new ArrayList<>();

  @Column(name = "current_stage_index", nullable = false)
  private Integer currentStageIndex = 0;

  @Column(name = "stage_entered_at")
  private LocalDateTime stageEnteredAt;

  @Column(name = "last_safe_percentage", nullable = false)
  private Integer lastSafePercentage = 0;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "halted_reason", length = 500)
  private String haltedReason;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public void addStage(RolloutStage stage) {
    stage.setSchedule(this);
    stages.add(stage);
  }

  /** The stage the flag is currently sitting at, empty once every stage has been completed. */
  public Optional<RolloutStage> currentStage() {
    if (currentStageIndex == null || currentStageIndex >= stages.size()) {
      return Optional.empty();
    }
    return Optional.of(stages.get(currentStageIndex));
  }

  /** Whether the current stage has been held for long enough to advance. */
  public boolean dwellElapsed(LocalDateTime now) {
    if (stageEnteredAt == null) {
      return true;
    }
    return currentStage()
        .map(stage -> stageEnteredAt.plusMinutes(stage.getDwellMinutes()).isBefore(now))
        .orElse(false);
  }

  public boolean hasRemainingStages() {
    return currentStageIndex != null && currentStageIndex < stages.size();
  }

  public void enterStage(int index, LocalDateTime now, int percentageEntered) {
    this.currentStageIndex = index;
    this.stageEnteredAt = now;
    this.lastSafePercentage = percentageEntered;
  }

  public void complete(LocalDateTime now) {
    this.status = RolloutStatus.COMPLETED;
    this.completedAt = now;
  }

  public void rollBack(String reason, LocalDateTime now) {
    this.status = RolloutStatus.ROLLED_BACK;
    this.haltedReason = reason;
    this.completedAt = now;
  }

  public Long getId() {
    return id;
  }

  public FeatureFlag getFeatureFlag() {
    return featureFlag;
  }

  public void setFeatureFlag(FeatureFlag featureFlag) {
    this.featureFlag = featureFlag;
  }

  public RolloutStatus getStatus() {
    return status;
  }

  public void setStatus(RolloutStatus status) {
    this.status = status;
  }

  public List<RolloutStage> getStages() {
    return List.copyOf(stages);
  }

  public Integer getCurrentStageIndex() {
    return currentStageIndex;
  }

  public LocalDateTime getStageEnteredAt() {
    return stageEnteredAt;
  }

  public void setStageEnteredAt(LocalDateTime stageEnteredAt) {
    this.stageEnteredAt = stageEnteredAt;
  }

  public Integer getLastSafePercentage() {
    return lastSafePercentage;
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

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public String getHaltedReason() {
    return haltedReason;
  }

  /** Lifecycle of a staged rollout. */
  public enum RolloutStatus {
    PENDING,
    RUNNING,
    PAUSED,
    COMPLETED,
    ROLLED_BACK
  }
}
