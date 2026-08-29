package com.rex.evaluation;

import java.util.List;

/**
 * One targeting condition on a flag.
 *
 * <p>Rules are ordered and the first match wins, which is what makes "everyone in Canada, plus ten
 * percent of everyone else" expressible. Percentage rollout alone cannot say that.
 */
public record TargetingRule(
    String attribute, RuleOperator operator, List<String> values, boolean enable) {

  public TargetingRule {
    values = List.copyOf(values);
  }

  @Override
  public List<String> values() {
    return List.copyOf(values);
  }

  /**
   * Whether this rule applies to the given attributes.
   *
   * <p>A user missing the targeted attribute falls through rather than matching. Treating absence
   * as a match would silently widen every rule to the whole population.
   */
  public boolean matches(java.util.Map<String, String> attributes) {
    String actual = attributes.get(attribute);
    return actual != null && operator.matches(actual, values);
  }
}
