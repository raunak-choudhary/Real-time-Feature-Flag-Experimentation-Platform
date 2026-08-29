package com.rex.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.event.FlagChangedEvent;
import com.rex.model.FeatureFlag;
import com.rex.support.PostgresIntegrationTest;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asserts that every operation changing what a flag serves also announces it.
 *
 * <p>The whole platform rests on a change reaching connected clients without a reload. A mutator
 * that saves but does not publish still passes any test that only reads the flag back, so the gap
 * is invisible from the service's own return value and only shows up as a stale client.
 */
@Transactional
@RecordApplicationEvents
class FlagChangeBroadcastTest extends PostgresIntegrationTest {

  @Autowired private FeatureFlagService flagService;
  @Autowired private ApplicationEvents events;

  private FeatureFlag subject(String name) {
    return flagService.createFeatureFlag(name, "d", "broadcast-test", "suite@rex.com");
  }

  private List<FlagChangedEvent> emittedBy(Consumer<FeatureFlag> operation, String name) {
    FeatureFlag flag = subject(name);
    long before = events.stream(FlagChangedEvent.class).count();
    operation.accept(flag);
    return events.stream(FlagChangedEvent.class).skip(before).toList();
  }

  @Test
  @DisplayName("creating a flag announces it")
  void creationAnnounces() {
    subject("broadcast_create");

    assertThat(events.stream(FlagChangedEvent.class))
        .anyMatch(e -> e.changeType() == FlagChangedEvent.ChangeType.CREATED);
  }

  @Test
  @DisplayName("toggling announces the new state, not the old one")
  void toggleAnnouncesTheNewState() {
    List<FlagChangedEvent> emitted =
        emittedBy(flag -> flagService.toggleFlag(flag.getId()), "broadcast_toggle");

    assertThat(emitted).singleElement().satisfies(e -> assertThat(e.enabled()).isTrue());
  }

  @Test
  @DisplayName("enabling and disabling by id announce")
  void enableAndDisableByIdAnnounce() {
    assertThat(emittedBy(f -> flagService.enableFlag(f.getId()), "broadcast_enable")).hasSize(1);
    assertThat(emittedBy(f -> flagService.disableFlag(f.getId()), "broadcast_disable")).hasSize(1);
  }

  @Test
  @DisplayName("enabling and disabling by name announce")
  void enableAndDisableByNameAnnounce() {
    assertThat(emittedBy(f -> flagService.enableFlagByName(f.getName()), "broadcast_enable_named"))
        .hasSize(1);
    assertThat(
            emittedBy(f -> flagService.disableFlagByName(f.getName()), "broadcast_disable_named"))
        .hasSize(1);
  }

  @Test
  @DisplayName("both rollout paths announce, not just the absolute one")
  void bothRolloutPathsAnnounce() {
    assertThat(emittedBy(f -> flagService.updateRolloutPercentage(f.getId(), 40), "broadcast_set"))
        .singleElement()
        .satisfies(e -> assertThat(e.rolloutPercentage()).isEqualTo(40));

    assertThat(emittedBy(f -> flagService.increaseRollout(f.getId(), 15), "broadcast_increase"))
        .singleElement()
        .satisfies(e -> assertThat(e.rolloutPercentage()).isEqualTo(15));
  }

  @Test
  @DisplayName("archiving announces, so clients stop serving an archived flag")
  void archiveAnnounces() {
    List<FlagChangedEvent> emitted =
        emittedBy(f -> flagService.deleteFeatureFlag(f.getId()), "broadcast_archive");

    assertThat(emitted)
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.changeType()).isEqualTo(FlagChangedEvent.ChangeType.ARCHIVED);
              assertThat(e.enabled()).isFalse();
            });
  }

  @Test
  @DisplayName("a bulk change announces once per flag rather than once per call")
  void bulkChangesAnnouncePerFlag() {
    List<Long> ids =
        List.of(subject("broadcast_bulk_one").getId(), subject("broadcast_bulk_two").getId());
    long before = events.stream(FlagChangedEvent.class).count();

    flagService.enableFlags(ids);

    assertThat(events.stream(FlagChangedEvent.class).skip(before)).hasSize(2);
  }
}
