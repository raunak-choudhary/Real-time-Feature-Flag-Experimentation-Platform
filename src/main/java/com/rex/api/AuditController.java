package com.rex.api;

import com.rex.api.dto.AuditEventResponse;
import com.rex.api.dto.StaleFlagResponse;
import com.rex.audit.AuditEvent;
import com.rex.audit.AuditService;
import com.rex.service.StaleFlag;
import com.rex.service.StaleFlagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The audit trail, and the flags nobody is using any more. */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

  private static final int DEFAULT_FEED_LIMIT = 50;

  private final AuditService auditService;
  private final StaleFlagService staleFlagService;

  public AuditController(AuditService auditService, StaleFlagService staleFlagService) {
    this.auditService = auditService;
    this.staleFlagService = staleFlagService;
  }

  @GetMapping
  public List<AuditEventResponse> recent(
      @RequestParam(defaultValue = "" + DEFAULT_FEED_LIMIT) int limit) {
    return auditService.recent(limit).stream().map(AuditController::toResponse).toList();
  }

  @GetMapping("/flags/{flagId}")
  public List<AuditEventResponse> forFlag(@PathVariable Long flagId) {
    return auditService.historyFor(flagId).stream().map(AuditController::toResponse).toList();
  }

  /**
   * Flags with no recorded evaluation inside the window.
   *
   * <p>Every flag platform accumulates permanent temporary flags. Surfacing them is the difference
   * between a system that grows and one that only grows.
   */
  @GetMapping("/stale-flags")
  public List<StaleFlagResponse> staleFlags(@RequestParam(defaultValue = "30") int days) {
    return staleFlagService.findStale(days).stream().map(AuditController::toResponse).toList();
  }

  private static StaleFlagResponse toResponse(StaleFlag flag) {
    return new StaleFlagResponse(
        flag.id(),
        flag.name(),
        flag.environment(),
        flag.enabled(),
        flag.rolloutPercentage(),
        flag.lastEvaluatedAt(),
        flag.daysSinceLastEvaluation());
  }

  private static AuditEventResponse toResponse(AuditEvent event) {
    return new AuditEventResponse(
        event.getId(),
        event.getActor(),
        event.getAction(),
        event.getTargetType(),
        event.getTargetId(),
        event.getTargetName(),
        event.getBeforeValue(),
        event.getAfterValue(),
        event.getReason(),
        event.getEnvironment(),
        event.getOccurredAt());
  }
}
