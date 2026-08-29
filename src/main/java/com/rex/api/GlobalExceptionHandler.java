package com.rex.api;

import com.rex.exception.DuplicateResourceException;
import com.rex.exception.InvalidStateTransitionException;
import com.rex.exception.ResourceNotFoundException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain failures into RFC 7807 problem responses.
 *
 * <p>Every error leaving the API has the same shape, so a client can handle failures generically
 * instead of pattern matching on prose. Unexpected exceptions are logged with their stack trace but
 * returned without one, since an internal trace on the wire is an information leak.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String PROBLEM_BASE = "https://rex-platform.dev/problems/";

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), "not-found");
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ProblemDetail handleDuplicate(DuplicateResourceException exception) {
    return problem(
        HttpStatus.CONFLICT, "Resource already exists", exception.getMessage(), "duplicate");
  }

  @ExceptionHandler(InvalidStateTransitionException.class)
  public ProblemDetail handleInvalidTransition(InvalidStateTransitionException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "Invalid state transition",
        exception.getMessage(),
        "invalid-transition");
  }

  /**
   * The service layer signals a lifecycle conflict this way, which is a conflict rather than a bad
   * request.
   */
  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalState(IllegalStateException exception) {
    return problem(
        HttpStatus.CONFLICT, "Operation not permitted", exception.getMessage(), "conflict");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
    return problem(
        HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), "invalid-request");
  }

  /**
   * Bean Validation failures are reported per field so a client can highlight the offending input.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "One or more fields are invalid",
            "validation");
    problem.setProperty("errors", fieldErrors);
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception exception) {
    logger.error("Unhandled exception serving request", exception);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "The request could not be completed",
        "internal");
  }

  private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create(PROBLEM_BASE + type));
    return problem;
  }
}
