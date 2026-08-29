package com.rex.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rex.model.FeatureFlag;
import com.rex.rollout.RolloutSchedule;
import com.rex.rollout.RolloutService;
import com.rex.rollout.RolloutStage;
import com.rex.service.FeatureFlagService;
import com.rex.support.PostgresIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AuditIntegrationTest extends PostgresIntegrationTest {

  @Autowired private AuditService auditService;
  @Autowired private FeatureFlagService flagService;
  @Autowired private RolloutService rolloutService;

  private FeatureFlag newFlag() {
    return flagService.createFeatureFlag(
        "audit_probe_" + System.nanoTime(),
        "audited flag",
        false,
        FeatureFlag.FlagStatus.ACTIVE,
        "production",
        0,
        "operator@rex.com");
  }

  @Test
  @DisplayName("toggling a flag writes an audit row carrying the resulting state")
  void toggleIsAudited() {
    FeatureFlag flag = newFlag();
    flagService.toggleFlag(flag.getId());

    List<AuditEvent> history = auditService.historyFor(flag.getId());

    assertThat(history).isNotEmpty();
    assertThat(history.get(0).getAction()).isEqualTo("TOGGLED");
    assertThat(history.get(0).getAfterValue()).contains("enabled=true");
    assertThat(history.get(0).getTargetName()).isEqualTo(flag.getName());
  }

  @Test
  @DisplayName("history is returned most recent first")
  void historyIsReverseChronological() {
    FeatureFlag flag = newFlag();
    flagService.toggleFlag(flag.getId());
    flagService.updateRolloutPercentage(flag.getId(), 50);

    List<AuditEvent> history = auditService.historyFor(flag.getId());

    assertThat(history).hasSizeGreaterThanOrEqualTo(2);
    assertThat(history.get(0).getOccurredAt()).isAfterOrEqualTo(history.get(1).getOccurredAt());
  }

  @Test
  @DisplayName("an automatic rollback is attributed to the scheduler, not to a person")
  void automaticRollbackIsAttributedToTheSystem() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.start(
            rolloutService
                .createSchedule(
                    flag.getId(),
                    List.of(new RolloutStage(0, 10, 60), new RolloutStage(1, 50, 60)),
                    "operator@rex.com")
                .getId());

    rolloutService.rollBack(schedule, "ERROR_RATE breached its threshold", LocalDateTime.now());

    AuditEvent rollback =
        auditService.historyFor(flag.getId()).stream()
            .filter(event -> "ROLLED_BACK".equals(event.getAction()))
            .findFirst()
            .orElseThrow();

    assertThat(rollback.getActor())
        .as("an automated action must be distinguishable from an operator action")
        .isEqualTo(AuditEvent.SYSTEM_ACTOR);
    assertThat(rollback.getReason()).contains("ERROR_RATE");
    assertThat(rollback.getBeforeValue()).isNotNull();
    assertThat(rollback.getAfterValue()).isNotNull();
  }

  @Test
  @DisplayName("a failed mutation leaves no audit row, since the two share a transaction")
  void failedMutationWritesNoAuditRow() {
    int before = auditService.recent(500).size();

    assertThatThrownBy(() -> flagService.toggleFlag(999_999L)).isInstanceOf(RuntimeException.class);

    assertThat(auditService.recent(500)).hasSize(before);
  }

  @Test
  @DisplayName("the recent feed is capped by the requested limit")
  void recentFeedRespectsLimit() {
    FeatureFlag flag = newFlag();
    for (int i = 0; i < 5; i++) {
      flagService.updateRolloutPercentage(flag.getId(), 10 + i);
    }

    assertThat(auditService.recent(3)).hasSize(3);
  }
}
