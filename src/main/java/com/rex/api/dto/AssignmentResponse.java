package com.rex.api.dto;

import java.time.LocalDateTime;

/** The variant a user was assigned to, and when. */
public record AssignmentResponse(
    String userId,
    Long experimentId,
    String experimentName,
    String variantName,
    String cohortType,
    Integer assignmentHash,
    LocalDateTime assignedAt) {}
