package com.rex.rollout;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for staged rollouts. */
public interface RolloutScheduleRepository extends JpaRepository<RolloutSchedule, Long> {

  List<RolloutSchedule> findByStatus(RolloutSchedule.RolloutStatus status);

  Optional<RolloutSchedule> findByFeatureFlagId(Long featureFlagId);

  boolean existsByFeatureFlagId(Long featureFlagId);
}
