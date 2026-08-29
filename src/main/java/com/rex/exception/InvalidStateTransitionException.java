package com.rex.exception;

/** A lifecycle transition was requested that the current state does not permit. Maps to 409. */
public class InvalidStateTransitionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidStateTransitionException(String message) {
    super(message);
  }
}
