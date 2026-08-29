package com.rex.api.dto;

import java.time.LocalDateTime;

/** One audit entry as returned by the API. */
public record AuditEventResponse(
    Long id,
    String actor,
    String action,
    String targetType,
    Long targetId,
    String targetName,
    String beforeValue,
    String afterValue,
    String reason,
    String environment,
    LocalDateTime occurredAt) {}
