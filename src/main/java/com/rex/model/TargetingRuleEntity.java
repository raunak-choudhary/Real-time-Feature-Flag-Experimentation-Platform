package com.rex.model;

import com.rex.evaluation.RuleOperator;
import com.rex.evaluation.TargetingRule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * Stored form of a targeting rule.
 *
 * <p>Separate from the {@link TargetingRule} record the engine evaluates, so the engine stays free
 * of persistence and remains unit testable without a database.
 */
@Entity
@Table(name = "targeting_rules")
public class TargetingRuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "feature_flag_id", nullable = false)
  private Long featureFlagId;

  @Column(name = "rule_order", nullable = false)
  private Integer ruleOrder;

  @Column(nullable = false, length = 100)
  private String attribute;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RuleOperator operator;

  @Column(name = "values_csv", nullable = false, length = 1000)
  private String valuesCsv;

  @Column(nullable = false)
  private Boolean enable;

  protected TargetingRuleEntity() {}

  public TargetingRuleEntity(
      Long featureFlagId,
      int ruleOrder,
      String attribute,
      RuleOperator operator,
      List<String> values,
      boolean enable) {
    this.featureFlagId = featureFlagId;
    this.ruleOrder = ruleOrder;
    this.attribute = attribute;
    this.operator = operator;
    this.valuesCsv = String.join(",", values);
    this.enable = enable;
  }

  /** Converts to the form the evaluation engine understands. */
  public TargetingRule toRule() {
    return new TargetingRule(
        attribute, operator, Arrays.asList(valuesCsv.split(",")), Boolean.TRUE.equals(enable));
  }

  public Long getId() {
    return id;
  }

  public Long getFeatureFlagId() {
    return featureFlagId;
  }

  public Integer getRuleOrder() {
    return ruleOrder;
  }

  public String getAttribute() {
    return attribute;
  }

  public RuleOperator getOperator() {
    return operator;
  }

  public String getValuesCsv() {
    return valuesCsv;
  }

  public Boolean getEnable() {
    return enable;
  }
}
