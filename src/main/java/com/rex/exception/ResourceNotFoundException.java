package com.rex.exception;

/** A requested resource does not exist. Maps to 404. */
public class ResourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(String resource, Object identifier) {
    super("%s '%s' was not found".formatted(resource, identifier));
  }
}
