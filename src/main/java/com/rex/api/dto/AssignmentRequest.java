package com.rex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for enrolling a user into an experiment. */
public record AssignmentRequest(
    @NotBlank(message = "userId is required")
        @Size(max = 255, message = "userId must be at most 255 characters")
        String userId,
    @Size(max = 255, message = "sessionId must be at most 255 characters") String sessionId) {}
