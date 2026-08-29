package com.rex.evaluation;

import java.util.List;
import java.util.Map;

/** Applies an ordered rule list, returning the first match. */
public final class RuleEvaluator {

  private RuleEvaluator() {}

  /**
   * Returns the outcome of the first matching rule, or empty when none match.
   *
   * <p>Empty means the caller should fall through to percentage rollout, which is what keeps flags
   * with no rules behaving exactly as they did before targeting existed.
   */
  public static java.util.Optional<Boolean> firstMatch(
      List<TargetingRule> rules, Map<String, String> attributes) {
    if (rules == null || rules.isEmpty() || attributes == null) {
      return java.util.Optional.empty();
    }
    return rules.stream()
        .filter(rule -> rule.matches(attributes))
        .findFirst()
        .map(TargetingRule::enable);
  }
}
