package com.rex.exception;

/**
 * A resource with the same natural key already exists. Maps to 409.
 *
 * <p>Distinct from a validation failure: the request was well formed, it just conflicts with the
 * current state, so a client should not retry it unchanged.
 */
public class DuplicateResourceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicateResourceException(String message) {
    super(message);
  }
}
