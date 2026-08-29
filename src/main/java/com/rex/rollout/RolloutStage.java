package com.rex.rollout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One step of a staged rollout: hold at this percentage for this long, then advance. */
@Entity
@Table(name = "rollout_stages")
public class RolloutStage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rollout_schedule_id", nullable = false)
  private RolloutSchedule schedule;

  @Column(name = "stage_order", nullable = false)
  private Integer stageOrder;

  @Column(name = "target_percentage", nullable = false)
  private Integer targetPercentage;

  @Column(name = "dwell_minutes", nullable = false)
  private Integer dwellMinutes;

  protected RolloutStage() {}

  public RolloutStage(int stageOrder, int targetPercentage, int dwellMinutes) {
    this.stageOrder = stageOrder;
    this.targetPercentage = targetPercentage;
    this.dwellMinutes = dwellMinutes;
  }

  public Long getId() {
    return id;
  }

  public RolloutSchedule getSchedule() {
    return schedule;
  }

  public void setSchedule(RolloutSchedule schedule) {
    this.schedule = schedule;
  }

  public Integer getStageOrder() {
    return stageOrder;
  }

  public Integer getTargetPercentage() {
    return targetPercentage;
  }

  public Integer getDwellMinutes() {
    return dwellMinutes;
  }
}
