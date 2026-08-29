-- Sample dataset.
--
-- Seeds an experiment with enough exposures and conversions that the analysis endpoint returns a
-- verdict rather than "insufficient data", and a flag partway through a staged rollout so the
-- audit feed has both a human and an automated actor in it. Without this a first run opens on
-- empty panels, which says nothing about whether any of it works.

-- A running experiment with a decided result: 20% control against 24% test over 1,000 each.
INSERT INTO experiments (name, description, hypothesis, success_metric, status, traffic_percentage,
                         control_variant_name, test_variant_name, confidence_level,
                         minimum_sample_size, current_sample_size, expected_improvement,
                         environment, created_by, created_at, updated_at, start_date)
VALUES ('checkout_button_contrast', 'Does a green checkout button convert better than blue',
        'A higher contrast button increases completed checkouts', 'conversion_rate',
        'RUNNING', 100, 'blue', 'green', 95.0, 900, 2000, 4.0,
        'production', 'growth@rex.com', CURRENT_TIMESTAMP - interval '9 day',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - interval '9 day');

-- Exposures. One row per user per variant, which is what the analysis counts as the denominator.
INSERT INTO metrics (user_id, experiment_id, event_type, event_name, variant_name, "timestamp",
                     environment, count_value)
SELECT 'user_c_' || generate_series,
       (SELECT id FROM experiments WHERE name = 'checkout_button_contrast'),
       'EXPERIMENT_EXPOSURE', 'variant_served', 'blue',
       CURRENT_TIMESTAMP - interval '8 day', 'production', 1
FROM generate_series(1, 1000);

INSERT INTO metrics (user_id, experiment_id, event_type, event_name, variant_name, "timestamp",
                     environment, count_value)
SELECT 'user_t_' || generate_series,
       (SELECT id FROM experiments WHERE name = 'checkout_button_contrast'),
       'EXPERIMENT_EXPOSURE', 'variant_served', 'green',
       CURRENT_TIMESTAMP - interval '8 day', 'production', 1
FROM generate_series(1, 1000);

-- Conversions: 200 of 1000 control, 240 of 1000 test. That is the worked example the statistics
-- tests verify by hand, so the reported result has arithmetic that is independently checked.
INSERT INTO metrics (user_id, experiment_id, event_type, event_name, variant_name, "timestamp",
                     environment, count_value, conversion_value)
SELECT 'user_c_' || generate_series,
       (SELECT id FROM experiments WHERE name = 'checkout_button_contrast'),
       'CONVERSION', 'checkout_completed', 'blue',
       CURRENT_TIMESTAMP - interval '7 day', 'production', 1, 1.0
FROM generate_series(1, 200);

INSERT INTO metrics (user_id, experiment_id, event_type, event_name, variant_name, "timestamp",
                     environment, count_value, conversion_value)
SELECT 'user_t_' || generate_series,
       (SELECT id FROM experiments WHERE name = 'checkout_button_contrast'),
       'CONVERSION', 'checkout_completed', 'green',
       CURRENT_TIMESTAMP - interval '7 day', 'production', 1, 1.0
FROM generate_series(1, 240);

-- A flag partway through a staged rollout, so the timeline has something to show.
INSERT INTO feature_flags (name, description, enabled, status, rollout_percentage, environment,
                           created_by, created_at, updated_at)
VALUES ('search_relevance_model', 'New relevance model for search results', true, 'ACTIVE', 25,
        'production', 'product@rex.com', CURRENT_TIMESTAMP - interval '2 day', CURRENT_TIMESTAMP);

INSERT INTO rollout_schedules (feature_flag_id, status, current_stage_index, stage_entered_at,
                               last_safe_percentage, created_by, created_at, updated_at)
VALUES ((SELECT id FROM feature_flags WHERE name = 'search_relevance_model'),
        'RUNNING', 1, CURRENT_TIMESTAMP - interval '1 hour', 5, 'product@rex.com',
        CURRENT_TIMESTAMP - interval '2 day', CURRENT_TIMESTAMP);

INSERT INTO rollout_stages (rollout_schedule_id, stage_order, target_percentage, dwell_minutes)
SELECT (SELECT id FROM rollout_schedules
        WHERE feature_flag_id = (SELECT id FROM feature_flags WHERE name = 'search_relevance_model')),
       stage_order, target_percentage, dwell_minutes
FROM (VALUES (0, 5, 120), (1, 25, 120), (2, 50, 120), (3, 100, 120))
       AS s(stage_order, target_percentage, dwell_minutes);

-- Audit entries, including one automated rollback so the feed shows both kinds of actor.
INSERT INTO audit_events (actor, action, target_type, target_id, target_name, before_value,
                          after_value, reason, environment, occurred_at)
VALUES ('product@rex.com', 'TOGGLED', 'FEATURE_FLAG',
        (SELECT id FROM feature_flags WHERE name = 'search_relevance_model'), 'search_relevance_model',
        'enabled=false rollout=0%', 'enabled=true rollout=5%', NULL, 'production',
        CURRENT_TIMESTAMP - interval '2 day'),
       ('system:rollout-scheduler', 'ROLLOUT_CHANGED', 'FEATURE_FLAG',
        (SELECT id FROM feature_flags WHERE name = 'search_relevance_model'), 'search_relevance_model',
        'rollout=5%', 'rollout=25%', NULL, 'production', CURRENT_TIMESTAMP - interval '1 hour'),
       ('system:rollout-scheduler', 'ROLLED_BACK', 'FEATURE_FLAG',
        (SELECT id FROM feature_flags WHERE name = 'mobile_push_notifications'),
        'mobile_push_notifications', 'rollout=50%', 'rollout=10%',
        'ERROR_RATE at 0.0850 breached its threshold of 0.0200 over 1240 observations',
        'production', CURRENT_TIMESTAMP - interval '5 hour');
