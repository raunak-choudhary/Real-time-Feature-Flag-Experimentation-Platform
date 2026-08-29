package com.rex.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RuleEvaluatorTest {

  private static TargetingRule rule(
      String attribute, RuleOperator operator, boolean enable, String... values) {
    return new TargetingRule(attribute, operator, List.of(values), enable);
  }

  @Test
  @DisplayName("a matching rule decides the outcome")
  void matchingRuleWins() {
    List<TargetingRule> rules = List.of(rule("country", RuleOperator.IN, true, "US", "CA"));

    assertThat(RuleEvaluator.firstMatch(rules, Map.of("country", "CA"))).contains(true);
  }

  @Test
  @DisplayName("a non matching user falls through so percentage rollout still applies")
  void nonMatchingFallsThrough() {
    List<TargetingRule> rules = List.of(rule("country", RuleOperator.IN, true, "US", "CA"));

    assertThat(RuleEvaluator.firstMatch(rules, Map.of("country", "DE"))).isEmpty();
  }

  @Test
  @DisplayName("with no rules defined, behaviour is unchanged from before targeting existed")
  void noRulesMeansFallThrough() {
    assertThat(RuleEvaluator.firstMatch(List.of(), Map.of("country", "US"))).isEmpty();
    assertThat(RuleEvaluator.firstMatch(null, Map.of("country", "US"))).isEmpty();
  }

  @Test
  @DisplayName("the first matching rule wins over a later contradictory one")
  void firstMatchWins() {
    List<TargetingRule> rules =
        List.of(
            rule("plan", RuleOperator.EQUALS, false, "free"),
            rule("country", RuleOperator.EQUALS, true, "US"));

    Optional<Boolean> outcome =
        RuleEvaluator.firstMatch(rules, Map.of("plan", "free", "country", "US"));

    assertThat(outcome).as("the earlier rule decides").contains(false);
  }

  @Test
  @DisplayName("a user missing the targeted attribute falls through rather than matching")
  void missingAttributeDoesNotMatch() {
    List<TargetingRule> rules = List.of(rule("country", RuleOperator.NOT_IN, true, "US"));

    assertThat(RuleEvaluator.firstMatch(rules, Map.of("plan", "pro")))
        .as("absence must not be treated as a match, or every rule widens to everyone")
        .isEmpty();
  }

  @ParameterizedTest(name = "{0} {1} {2} is {3}")
  @CsvSource({
    "pro,     EQUALS,     pro,   true",
    "pro,     EQUALS,     free,  false",
    "pro,     NOT_EQUALS, free,  true",
    "beta,    CONTAINS,   et,    true",
    "beta,    CONTAINS,   xy,    false",
    "42,      GREATER_THAN, 10,  true",
    "5,       GREATER_THAN, 10,  false",
    "5,       LESS_THAN,    10,  true",
  })
  @DisplayName("each operator compares as expected")
  void operatorsBehave(String actual, RuleOperator operator, String expected, boolean shouldMatch) {
    List<TargetingRule> rules = List.of(rule("attr", operator, true, expected));

    assertThat(RuleEvaluator.firstMatch(rules, Map.of("attr", actual)).isPresent())
        .isEqualTo(shouldMatch);
  }

  @ParameterizedTest(name = "version {0} vs {1} orders correctly")
  @CsvSource({
    "1.10.0, 1.9.0,  true",
    "1.9.0,  1.10.0, false",
    "2.0.0,  1.99.9, true",
    "1.0.0,  1.0.0,  true",
    "1.0,    1.0.0,  true",
  })
  @DisplayName("version comparison is numeric, so 1.10.0 sorts above 1.9.0 rather than below")
  void versionComparisonIsNumericNotLexical(String actual, String threshold, boolean shouldMatch) {
    List<TargetingRule> rules =
        List.of(rule("appVersion", RuleOperator.VERSION_GREATER_OR_EQUAL, true, threshold));

    assertThat(RuleEvaluator.firstMatch(rules, Map.of("appVersion", actual)).isPresent())
        .as("lexical comparison would get 1.10.0 against 1.9.0 wrong")
        .isEqualTo(shouldMatch);
  }

  @Test
  @DisplayName("a non numeric value against a numeric operator does not match rather than throwing")
  void nonNumericValueDoesNotThrow() {
    List<TargetingRule> rules = List.of(rule("age", RuleOperator.GREATER_THAN, true, "18"));

    assertThat(RuleEvaluator.firstMatch(rules, Map.of("age", "not-a-number"))).isEmpty();
  }

  @Test
  @DisplayName("a rule can disable for a segment while others remain eligible")
  void ruleCanExclude() {
    List<TargetingRule> rules = List.of(rule("region", RuleOperator.EQUALS, false, "EU"));

    assertThat(RuleEvaluator.firstMatch(rules, Map.of("region", "EU"))).contains(false);
    assertThat(RuleEvaluator.firstMatch(rules, Map.of("region", "US"))).isEmpty();
  }

  @Test
  @DisplayName("the values list cannot be mutated after the rule is built")
  void valuesAreImmutable() {
    TargetingRule targeting = rule("country", RuleOperator.IN, true, "US");

    assertThat(targeting.values()).containsExactly("US");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> targeting.values().add("CA"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
