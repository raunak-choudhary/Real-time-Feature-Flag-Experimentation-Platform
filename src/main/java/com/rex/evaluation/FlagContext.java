package com.rex.evaluation;

/**
 * The flag state an evaluation needs, decoupled from the JPA entity.
 *
 * <p>Taking a record rather than the entity is what keeps the engine free of persistence: it can be
 * evaluated in a unit test with no database, and the ArchUnit rules enforce that it stays that way.
 */
public record FlagContext(
    String name,
    boolean enabled,
    String environment,
    int rolloutPercentage,
    java.util.List<TargetingRule> rules) {

  public FlagContext {
    rules = rules == null ? java.util.List.of() : java.util.List.copyOf(rules);
  }

  /** A flag with no targeting rules, which is the common case. */
  public FlagContext(String name, boolean enabled, String environment, int rolloutPercentage) {
    this(name, enabled, environment, rolloutPercentage, java.util.List.of());
  }

  @Override
  public java.util.List<TargetingRule> rules() {
    return java.util.List.copyOf(rules);
  }
}
