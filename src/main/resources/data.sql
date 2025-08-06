-- ================================
-- REX PLATFORM - SAMPLE DATA INITIALIZATION
-- Real-time Feature Flag & Experimentation Platform
-- ================================

-- ================================
-- FEATURE FLAGS SAMPLE DATA
-- ================================

-- Sample Feature Flags for different scenarios
INSERT INTO feature_flags (name, description, enabled, status, rollout_percentage, environment, created_by, created_at, updated_at) VALUES
                                                                                                                                        ('dark_mode', 'Enable dark mode theme for better user experience', true, 'ACTIVE', 100, 'production', 'admin@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                        ('new_checkout_flow', 'Enhanced checkout process with one-click payments', false, 'INACTIVE', 0, 'production', 'product@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                        ('premium_features', 'Access to premium features for subscribed users', true, 'ACTIVE', 50, 'production', 'admin@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                        ('beta_dashboard', 'New analytics dashboard with real-time metrics', true, 'ACTIVE', 25, 'staging', 'dev@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                        ('mobile_push_notifications', 'Push notifications for mobile app users', false, 'DEPRECATED', 0, 'production', 'mobile@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ================================
-- EXPERIMENTS SAMPLE DATA
-- ================================

-- Sample A/B Testing Experiments
INSERT INTO experiments (name, description, hypothesis, control_variant_name, test_variant_name, traffic_percentage, status, confidence_level, minimum_sample_size, environment, success_metric, created_by, created_at, updated_at, start_date) VALUES
                                                                                                                                                                                                                                                     ('homepage_cta_test',
                                                                                                                                                                                                                                                      'Testing different call-to-action button colors on homepage',
                                                                                                                                                                                                                                                      'Red CTA buttons will increase click-through rates by 15% compared to blue buttons',
                                                                                                                                                                                                                                                      'blue_button', 'red_button', 50, 'RUNNING', 95.0, 1000, 'production', 'click_through_rate',
                                                                                                                                                                                                                                                      'growth@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                                                                                                                                                                     ('pricing_page_layout',
                                                                                                                                                                                                                                                      'Comparing vertical vs horizontal pricing table layouts',
                                                                                                                                                                                                                                                      'Horizontal layout will improve conversion rates by 20%',
                                                                                                                                                                                                                                                      'vertical_layout', 'horizontal_layout', 30, 'READY', 90.0, 500, 'production', 'conversion_rate',
                                                                                                                                                                                                                                                      'product@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),

                                                                                                                                                                                                                                                     ('onboarding_flow_test',
                                                                                                                                                                                                                                                      'Testing 3-step vs 5-step user onboarding process',
                                                                                                                                                                                                                                                      'Simplified 3-step onboarding will reduce drop-off rates by 25%',
                                                                                                                                                                                                                                                      'five_step_onboarding', 'three_step_onboarding', 75, 'DRAFT', 85.0, 2000, 'staging', 'completion_rate',
                                                                                                                                                                                                                                                      'ux@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),

                                                                                                                                                                                                                                                     ('email_subject_test',
                                                                                                                                                                                                                                                      'Testing personalized vs generic email subject lines',
                                                                                                                                                                                                                                                      'Personalized subject lines will increase open rates by 30%',
                                                                                                                                                                                                                                                      'generic_subject', 'personalized_subject', 40, 'COMPLETED', 99.0, 5000, 'production', 'email_open_rate',
                                                                                                                                                                                                                                                      'marketing@rex.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('DAY', -7, CURRENT_TIMESTAMP));

-- ================================
-- USER COHORTS SAMPLE DATA
-- ================================

-- Sample User Assignments to Experiments
INSERT INTO user_cohorts (user_id, experiment_id, variant_name, cohort_type, assignment_method, assigned_at, is_active, assignment_hash, environment, session_id) VALUES
-- Homepage CTA Test Assignments
('user_001', 1, 'blue_button', 'CONTROL', 'HASH_BASED', CURRENT_TIMESTAMP, true, 12345, 'production', 'session_001'),
('user_002', 1, 'red_button', 'TREATMENT', 'HASH_BASED', CURRENT_TIMESTAMP, true, 67890, 'production', 'session_002'),
('user_003', 1, 'blue_button', 'CONTROL', 'HASH_BASED', CURRENT_TIMESTAMP, true, 11111, 'production', 'session_003'),
('user_004', 1, 'red_button', 'TREATMENT', 'HASH_BASED', CURRENT_TIMESTAMP, true, 22222, 'production', 'session_004'),
('user_005', 1, 'blue_button', 'CONTROL', 'HASH_BASED', CURRENT_TIMESTAMP, true, 33333, 'production', 'session_005'),

-- Email Subject Test Assignments (Completed Experiment)
('user_006', 4, 'generic_subject', 'CONTROL', 'PERCENTAGE_BASED', DATEADD('DAY', -10, CURRENT_TIMESTAMP), false, 44444, 'production', 'session_006'),
('user_007', 4, 'personalized_subject', 'TREATMENT', 'PERCENTAGE_BASED', DATEADD('DAY', -10, CURRENT_TIMESTAMP), false, 55555, 'production', 'session_007'),
('user_008', 4, 'generic_subject', 'CONTROL', 'PERCENTAGE_BASED', DATEADD('DAY', -8, CURRENT_TIMESTAMP), false, 66666, 'production', 'session_008'),

-- Onboarding Flow Test Assignments (Draft - for testing)
('user_009', 3, 'five_step_onboarding', 'CONTROL', 'RANDOM', CURRENT_TIMESTAMP, true, 77777, 'staging', 'session_009'),
('user_010', 3, 'three_step_onboarding', 'TREATMENT', 'RANDOM', CURRENT_TIMESTAMP, true, 88888, 'staging', 'session_010');

-- ================================
-- METRICS SAMPLE DATA
-- ================================

-- Sample Metrics and Events
INSERT INTO metrics (user_id, event_type, event_name, feature_flag_id, experiment_id, variant_name, timestamp, count_value, event_value, environment, session_id, page_url, user_agent, device_type, platform) VALUES
-- Feature Flag Events
('user_001', 'FLAG_EXPOSURE', 'dark_mode_shown', 1, NULL, NULL, CURRENT_TIMESTAMP, 1, NULL, 'production', 'session_001', '/dashboard', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 'desktop', 'web'),
('user_002', 'FLAG_ENABLED', 'premium_features_accessed', 3, NULL, NULL, CURRENT_TIMESTAMP, 1, 1.0, 'production', 'session_002', '/premium', 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X)', 'mobile', 'ios'),
('user_003', 'FLAG_EXPOSURE', 'beta_dashboard_shown', 4, NULL, NULL, CURRENT_TIMESTAMP, 1, NULL, 'staging', 'session_003', '/analytics', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'desktop', 'web'),

-- Experiment Events - Homepage CTA Test
('user_001', 'EXPERIMENT_EXPOSURE', 'homepage_cta_viewed', NULL, 1, 'blue_button', CURRENT_TIMESTAMP, 1, NULL, 'production', 'session_001', '/home', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 'desktop', 'web'),
('user_002', 'EXPERIMENT_EXPOSURE', 'homepage_cta_viewed', NULL, 1, 'red_button', CURRENT_TIMESTAMP, 1, NULL, 'production', 'session_002', '/home', 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X)', 'mobile', 'ios'),
('user_001', 'CLICK', 'cta_button_clicked', NULL, 1, 'blue_button', DATEADD('SECOND', 5, CURRENT_TIMESTAMP), 1, 1.0, 'production', 'session_001', '/home', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 'desktop', 'web'),
('user_002', 'CLICK', 'cta_button_clicked', NULL, 1, 'red_button', DATEADD('SECOND', 3, CURRENT_TIMESTAMP), 1, 1.0, 'production', 'session_002', '/home', 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X)', 'mobile', 'ios'),

-- Conversion Events
('user_001', 'CONVERSION', 'signup_completed', NULL, 1, 'blue_button', DATEADD('MINUTE', 2, CURRENT_TIMESTAMP), 1, 29.99, 'production', 'session_001', '/signup/complete', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 'desktop', 'web'),
('user_004', 'CONVERSION', 'purchase_completed', NULL, 1, 'red_button', DATEADD('MINUTE', 5, CURRENT_TIMESTAMP), 1, 99.99, 'production', 'session_004', '/checkout/success', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'desktop', 'web'),

-- Email Campaign Events (Completed Experiment)
('user_006', 'EXPERIMENT_EXPOSURE', 'email_opened', NULL, 4, 'generic_subject', DATEADD('DAY', -10, CURRENT_TIMESTAMP), 1, NULL, 'production', 'session_006', '/email/campaign', 'Email Client', 'mobile', 'email'),
('user_007', 'EXPERIMENT_EXPOSURE', 'email_opened', NULL, 4, 'personalized_subject', DATEADD('DAY', -10, CURRENT_TIMESTAMP), 1, NULL, 'production', 'session_007', '/email/campaign', 'Email Client', 'desktop', 'email'),
('user_007', 'CLICK', 'email_link_clicked', NULL, 4, 'personalized_subject', DATEADD('DAY', -10, CURRENT_TIMESTAMP), 1, 1.0, 'production', 'session_007', '/landing/email', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 'desktop', 'web'),

-- Page View Events
('user_003', 'PAGE_VIEW', 'dashboard_visited', NULL, NULL, NULL, CURRENT_TIMESTAMP, 1, NULL, 'production', 'session_003', '/dashboard', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'desktop', 'web'),
('user_005', 'PAGE_VIEW', 'pricing_visited', NULL, NULL, NULL, CURRENT_TIMESTAMP, 1, NULL, 'production', 'session_005', '/pricing', 'Mozilla/5.0 (Android 11; Mobile)', 'mobile', 'android'),

-- Error Events
('user_008', 'ERROR', 'checkout_error', 2, NULL, NULL, CURRENT_TIMESTAMP, 1, NULL, 'production', 'session_008', '/checkout', 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X)', 'mobile', 'ios'),

-- Performance Metrics
('user_009', 'LOAD_TIME', 'page_load_measured', NULL, NULL, NULL, CURRENT_TIMESTAMP, 1, 2.35, 'staging', 'session_009', '/onboarding', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 'desktop', 'web'),
('user_010', 'API_RESPONSE_TIME', 'api_call_measured', NULL, NULL, NULL, CURRENT_TIMESTAMP, 1, 0.45, 'staging', 'session_010', '/api/user/profile', 'Mobile App', 'mobile', 'ios');

-- ================================
-- UPDATE EXPOSURE TRACKING
-- ================================

-- Update user cohorts with exposure information
UPDATE user_cohorts SET
                        first_exposure_at = DATEADD('MINUTE', -CAST(RAND() * 60 AS INTEGER), CURRENT_TIMESTAMP),
                        last_exposure_at = CURRENT_TIMESTAMP,
                        exposure_count = CAST(1 + RAND() * 5 AS INTEGER)
WHERE id IN (1, 2, 3, 4, 5);

-- Update experiments with current sample sizes
UPDATE experiments SET
    current_sample_size = (
        SELECT COUNT(*)
        FROM user_cohorts uc
        WHERE uc.experiment_id = experiments.id
    )
WHERE id IN (1, 2, 3, 4);

-- ================================
-- SUMMARY OF SAMPLE DATA
-- ================================
--
-- Feature Flags: 5 flags (active, inactive, deprecated)
-- Experiments: 4 experiments (running, ready, draft, completed)
-- User Cohorts: 10 user assignments across experiments
-- Metrics: 15+ events covering flags, experiments, conversions, errors
--
-- This data provides:
-- - Real A/B testing scenarios
-- - Feature flag usage patterns
-- - User engagement metrics
-- - Conversion tracking
-- - Error monitoring
-- - Performance metrics
-- ================================