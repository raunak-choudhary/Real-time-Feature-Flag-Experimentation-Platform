package com.rex.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * One recorded configuration change.
 *
 * <p>Append only. There is no update path and no delete path, because an audit trail that can be
 * edited is not an audit trail.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

  /** The actor recorded for changes the platform made on its own. */
  public static final String SYSTEM_ACTOR = "system:rollout-scheduler";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String actor;

  @Column(nullable = false, length = 50)
  private String action;

  @Column(name = "target_type", nullable = false, length = 50)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "target_name", length = 100)
  private String targetName;

  @Column(name = "before_value", length = 500)
  private String beforeValue;

  @Column(name = "after_value", length = 500)
  private String afterValue;

  @Column(length = 500)
  private String reason;

  @Column(length = 50)
  private String environment;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  protected AuditEvent() {}

  private AuditEvent(Builder builder) {
    this.actor = builder.actor;
    this.action = builder.action;
    this.targetType = builder.targetType;
    this.targetId = builder.targetId;
    this.targetName = builder.targetName;
    this.beforeValue = builder.beforeValue;
    this.afterValue = builder.afterValue;
    this.reason = builder.reason;
    this.environment = builder.environment;
    this.occurredAt = LocalDateTime.now();
  }

  public static Builder builder(String actor, String action, String targetType) {
    return new Builder(actor, action, targetType);
  }

  public Long getId() {
    return id;
  }

  public String getActor() {
    return actor;
  }

  public String getAction() {
    return action;
  }

  public String getTargetType() {
    return targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public String getTargetName() {
    return targetName;
  }

  public String getBeforeValue() {
    return beforeValue;
  }

  public String getAfterValue() {
    return afterValue;
  }

  public String getReason() {
    return reason;
  }

  public String getEnvironment() {
    return environment;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  /** Fluent construction, since an audit row has many optional fields and no sensible ordering. */
  public static final class Builder {
    private final String actor;
    private final String action;
    private final String targetType;
    private Long targetId;
    private String targetName;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private String environment;

    private Builder(String actor, String action, String targetType) {
      this.actor = actor;
      this.action = action;
      this.targetType = targetType;
    }

    public Builder target(Long id, String name) {
      this.targetId = id;
      this.targetName = name;
      return this;
    }

    public Builder change(String before, String after) {
      this.beforeValue = before;
      this.afterValue = after;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder environment(String environment) {
      this.environment = environment;
      return this;
    }

    public AuditEvent build() {
      return new AuditEvent(this);
    }
  }
}
