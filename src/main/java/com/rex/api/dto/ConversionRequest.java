package com.rex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A client reported conversion, attributed to whichever variant the user was assigned. */
public record ConversionRequest(
    @NotBlank(message = "userId is required")
        @Size(max = 255, message = "userId must be at most 255 characters")
        String userId,
    @NotNull(message = "experimentId is required") Long experimentId,
    @Size(max = 255, message = "eventName must be at most 255 characters") String eventName,
    Double value,
    @Size(max = 255, message = "sessionId must be at most 255 characters") String sessionId) {}
