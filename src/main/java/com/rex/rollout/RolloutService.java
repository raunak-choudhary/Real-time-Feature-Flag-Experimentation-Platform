package com.rex.rollout;

import com.rex.audit.AuditService;
import com.rex.event.FlagChangedEvent;
import com.rex.exception.DuplicateResourceException;
import com.rex.exception.InvalidStateTransitionException;
import com.rex.exception.ResourceNotFoundException;
import com.rex.model.FeatureFlag;
import com.rex.realtime.ChangePublisher;
import com.rex.repository.FeatureFlagRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and advances staged rollouts.
 *
 * <p>Broadcasts are issued directly rather than through the transactional event listener. The
 * scheduler runs outside a request transaction, and an after commit listener silently does nothing
 * when no transaction is active, so relying on it here would leave every connected client unaware
 * that a stage had advanced.
 */
@Service
@Transactional
public class RolloutService {

  private static final Logger logger = LoggerFactory.getLogger(RolloutService.class);

  private final RolloutScheduleRepository scheduleRepository;
  private final FeatureFlagRepository flagRepository;
  private final ChangePublisher changePublisher;
  private final GuardrailEvaluator guardrailEvaluator;
  private final AuditService auditService;

  public RolloutService(
      RolloutScheduleRepository scheduleRepository,
      FeatureFlagRepository flagRepository,
      ChangePublisher changePublisher,
      GuardrailEvaluator guardrailEvaluator,
      AuditService auditService) {
    this.scheduleRepository = scheduleRepository;
    this.flagRepository = flagRepository;
    this.changePublisher = changePublisher;
    this.guardrailEvaluator = guardrailEvaluator;
    this.auditService = auditService;
  }

  /**
   * Creates a schedule for a flag. One at a time, so two rollouts cannot fight over a percentage.
   */
  public RolloutSchedule createSchedule(Long flagId, List<RolloutStage> stages, String createdBy) {
    if (stages.isEmpty()) {
      throw new IllegalArgumentException("a rollout needs at least one stage");
    }
    validateAscending(stages);

    FeatureFlag flag =
        flagRepository
            .findById(flagId)
            .orElseThrow(() -> new ResourceNotFoundException("Feature flag", flagId));

    if (scheduleRepository.existsByFeatureFlagId(flagId)) {
      throw new DuplicateResourceException(
          "Feature flag '%s' already has a rollout schedule".formatted(flag.getName()));
    }

    RolloutSchedule schedule = new RolloutSchedule();
    schedule.setFeatureFlag(flag);
    schedule.setCreatedBy(createdBy);
    stages.forEach(schedule::addStage);

    return scheduleRepository.save(schedule);
  }

  /** Starts a rollout by entering its first stage immediately. */
  public RolloutSchedule start(Long scheduleId) {
    RolloutSchedule schedule = require(scheduleId);
    if (schedule.getStatus() != RolloutSchedule.RolloutStatus.PENDING
        && schedule.getStatus() != RolloutSchedule.RolloutStatus.PAUSED) {
      throw new InvalidStateTransitionException(
          "Cannot start a rollout in state " + schedule.getStatus());
    }

    schedule.setStatus(RolloutSchedule.RolloutStatus.RUNNING);
    applyStage(schedule, 0);
    return scheduleRepository.save(schedule);
  }

  public RolloutSchedule pause(Long scheduleId) {
    RolloutSchedule schedule = require(scheduleId);
    if (schedule.getStatus() != RolloutSchedule.RolloutStatus.RUNNING) {
      throw new InvalidStateTransitionException(
          "Cannot pause a rollout in state " + schedule.getStatus());
    }
    schedule.setStatus(RolloutSchedule.RolloutStatus.PAUSED);
    return scheduleRepository.save(schedule);
  }

  /**
   * Advances to the next stage, or completes the rollout when none remain.
   *
   * <p>Idempotent by dwell time: calling this before the current stage has been held long enough
   * changes nothing, so a duplicate sweep cannot skip a stage.
   */
  public boolean advanceIfDue(RolloutSchedule schedule, LocalDateTime now) {
    return advanceIfDue(schedule, now, List.of());
  }

  /**
   * Advances a stage unless a guardrail says otherwise.
   *
   * <p>Guardrails are checked before advancing, not after. Advancing first and measuring later
   * would expose the next tranche of users to a problem already visible in the current one.
   */
  public boolean advanceIfDue(
      RolloutSchedule schedule, LocalDateTime now, List<Guardrail> guardrails) {

    if (schedule.getStatus() != RolloutSchedule.RolloutStatus.RUNNING) {
      return false;
    }
    if (!schedule.dwellElapsed(now)) {
      return false;
    }

    if (!guardrails.isEmpty()) {
      List<GuardrailVerdict> verdicts =
          guardrailEvaluator.evaluate(
              schedule.getFeatureFlag().getId(), guardrails, schedule.getStageEnteredAt(), now);

      GuardrailVerdict breach =
          verdicts.stream()
              .filter(v -> v.status() == GuardrailVerdict.Status.BREACHED)
              .findFirst()
              .orElse(null);

      if (breach != null) {
        rollBack(schedule, breach.describe(), now);
        return true;
      }

      // Insufficient or unreadable data blocks the advance without rolling back. Failing open
      // here would let a monitoring outage quietly turn a guarded rollout into an unguarded one.
      if (verdicts.stream().anyMatch(GuardrailVerdict::blocksAdvance)) {
        logger.info(
            "Rollout {} held: {}",
            schedule.getId(),
            verdicts.stream()
                .filter(GuardrailVerdict::blocksAdvance)
                .map(GuardrailVerdict::describe)
                .toList());
        return false;
      }
    }

    int next = schedule.getCurrentStageIndex() + 1;
    if (next >= schedule.getStages().size()) {
      schedule.complete(now);
      scheduleRepository.save(schedule);
      logger.info("Rollout {} completed", schedule.getId());
      return true;
    }

    applyStage(schedule, next);
    scheduleRepository.save(schedule);
    return true;
  }

  /** Reverts the flag to the last percentage that ran without a guardrail breach. */
  public RolloutSchedule rollBack(RolloutSchedule schedule, String reason, LocalDateTime now) {
    FeatureFlag flag = schedule.getFeatureFlag();
    int safePercentage = schedule.getLastSafePercentage();
    int previousPercentage = flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0;

    flag.setRolloutPercentage(safePercentage);
    flagRepository.save(flag);

    schedule.rollBack(reason, now);
    RolloutSchedule saved = scheduleRepository.save(schedule);

    // Attributed to the scheduler rather than a user, so an automatic rollback is traceable.
    auditService.recordAutomated(
        "ROLLED_BACK",
        flag.getId(),
        flag.getName(),
        "rollout=" + previousPercentage + "%",
        "rollout=" + safePercentage + "%",
        reason);

    logger.warn(
        "Rollout {} rolled back to {}% for flag '{}': {}",
        schedule.getId(), safePercentage, flag.getName(), reason);
    broadcast(flag);
    return saved;
  }

  public List<RolloutSchedule> running() {
    return scheduleRepository.findByStatus(RolloutSchedule.RolloutStatus.RUNNING);
  }

  public RolloutSchedule require(Long scheduleId) {
    return scheduleRepository
        .findById(scheduleId)
        .orElseThrow(() -> new ResourceNotFoundException("Rollout schedule", scheduleId));
  }

  /**
   * Moves the flag to a stage's percentage.
   *
   * <p>The database is updated before the broadcast, so a broker failure cannot leave clients and
   * the database disagreeing about the current percentage.
   */
  private void applyStage(RolloutSchedule schedule, int stageIndex) {
    RolloutStage stage = schedule.getStages().get(stageIndex);
    FeatureFlag flag = schedule.getFeatureFlag();

    int previousPercentage = flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0;
    flag.setRolloutPercentage(stage.getTargetPercentage());
    flag.setEnabled(true);
    flagRepository.save(flag);

    schedule.enterStage(stageIndex, LocalDateTime.now(), previousPercentage);
    logger.info(
        "Rollout {} entered stage {} at {}%",
        schedule.getId(), stageIndex, stage.getTargetPercentage());
    broadcast(flag);
  }

  private void broadcast(FeatureFlag flag) {
    changePublisher.broadcast(
        FlagChangedEvent.of(
            flag.getId(),
            flag.getName(),
            flag.getEnvironment(),
            Boolean.TRUE.equals(flag.getEnabled()),
            flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0,
            FlagChangedEvent.ChangeType.ROLLOUT_CHANGED));
  }

  /** Stages must increase, since a rollout that goes backwards would revoke a live feature. */
  private void validateAscending(List<RolloutStage> stages) {
    for (int i = 1; i < stages.size(); i++) {
      if (stages.get(i).getTargetPercentage() <= stages.get(i - 1).getTargetPercentage()) {
        throw new IllegalArgumentException(
            "rollout stages must increase; stage %d is not above stage %d".formatted(i, i - 1));
      }
    }
  }
}
