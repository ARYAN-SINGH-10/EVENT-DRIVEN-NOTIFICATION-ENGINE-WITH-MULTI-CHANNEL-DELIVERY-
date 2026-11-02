-- =============================================================================
-- Flyway Database Schema Migration V1
-- Event-Driven Notification Engine — Core Schema for PostgreSQL
-- Matches all 15 JPA Entity definitions for strict Hibernate validation
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------------
-- 1. Notifications Table (NotificationEntity)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    recipient_address VARCHAR(255) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subject VARCHAR(500),
    body TEXT,
    template_id UUID,
    template_variables TEXT,
    event_type VARCHAR(100),
    priority VARCHAR(20) DEFAULT 'NORMAL',
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message TEXT,
    external_message_id VARCHAR(255),
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    expires_at TIMESTAMP,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications (status);
CREATE INDEX IF NOT EXISTS idx_notification_channel ON notifications (channel_type);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications (created_at);
CREATE INDEX IF NOT EXISTS idx_notification_scheduled_at ON notifications (scheduled_at);

-- -----------------------------------------------------------------------------
-- 2. Dead Letter Queue Table (DeadLetterQueueEntry)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    user_id VARCHAR(100),
    channel_type VARCHAR(20),
    original_payload TEXT,
    failure_reason TEXT,
    failure_count INT DEFAULT 1,
    max_retry_attempts INT DEFAULT 5,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_RETRY',
    next_retry_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dlq_notification_id ON dead_letter_queue (notification_id);
CREATE INDEX IF NOT EXISTS idx_dlq_status ON dead_letter_queue (status);
CREATE INDEX IF NOT EXISTS idx_dlq_next_retry ON dead_letter_queue (next_retry_at);
CREATE INDEX IF NOT EXISTS idx_dlq_channel ON dead_letter_queue (channel_type);

-- -----------------------------------------------------------------------------
-- 3. Notification Events Table (NotificationEvent)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    user_id VARCHAR(100),
    source VARCHAR(100),
    correlation_id VARCHAR(100),
    payload TEXT,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP,
    notification_id UUID,
    error_message TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_event_correlation_id UNIQUE (correlation_id)
);

CREATE INDEX IF NOT EXISTS idx_event_type ON notification_events (event_type);
CREATE INDEX IF NOT EXISTS idx_event_user_id ON notification_events (user_id);
CREATE INDEX IF NOT EXISTS idx_event_source ON notification_events (source);
CREATE INDEX IF NOT EXISTS idx_event_created_at ON notification_events (created_at);
CREATE INDEX IF NOT EXISTS idx_event_processed ON notification_events (processed);
CREATE INDEX IF NOT EXISTS idx_event_correlation_id ON notification_events (correlation_id);

-- -----------------------------------------------------------------------------
-- 4. Notification Templates Table (NotificationTemplate)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(1000),
    channel_type VARCHAR(20) NOT NULL,
    event_type VARCHAR(100),
    subject_template VARCHAR(500),
    body_template TEXT NOT NULL,
    html_body_template TEXT,
    available_variables TEXT,
    locale VARCHAR(10) DEFAULT 'en-US',
    version INT DEFAULT 1,
    active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_template_name ON notification_templates (name);
CREATE INDEX IF NOT EXISTS idx_template_channel ON notification_templates (channel_type);
CREATE INDEX IF NOT EXISTS idx_template_event_type ON notification_templates (event_type);
CREATE INDEX IF NOT EXISTS idx_template_active ON notification_templates (active);

-- -----------------------------------------------------------------------------
-- 5. Template Versions Table (TemplateVersion)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS template_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    version INT NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    event_type VARCHAR(100),
    locale VARCHAR(10),
    subject_template VARCHAR(500),
    body_template TEXT NOT NULL,
    html_body_template TEXT,
    created_by VARCHAR(100),
    change_summary VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tv_template_id ON template_versions (template_id);
CREATE INDEX IF NOT EXISTS idx_tv_template_version ON template_versions (template_id, version);

-- -----------------------------------------------------------------------------
-- 6. User Preferences Table (UserPreference)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    opted_out_event_types TEXT,
    frequency VARCHAR(20) DEFAULT 'IMMEDIATE',
    quiet_hours_start VARCHAR(5),
    quiet_hours_end VARCHAR(5),
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    priority_override VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_pref_user_channel UNIQUE (user_id, channel_type)
);

CREATE INDEX IF NOT EXISTS idx_pref_user_id ON user_preferences (user_id);

-- -----------------------------------------------------------------------------
-- 7. User Profiles Table (UserProfile)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone_number VARCHAR(20),
    locale VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(60) DEFAULT 'Asia/Kolkata',
    preferred_currency VARCHAR(5) DEFAULT 'INR',
    risk_profile VARCHAR(20) DEFAULT 'MODERATE',
    account_type VARCHAR(20) DEFAULT 'COMBINED',
    kyc_status VARCHAR(20) DEFAULT 'VERIFIED',
    portfolio_value NUMERIC(18,2) DEFAULT 0.00,
    invested_amount NUMERIC(18,2) DEFAULT 0.00,
    available_margin NUMERIC(18,2) DEFAULT 0.00,
    used_margin NUMERIC(18,2) DEFAULT 0.00,
    premium_account BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_up_user_id ON user_profiles (user_id);
CREATE INDEX IF NOT EXISTS idx_up_locale ON user_profiles (locale);
CREATE INDEX IF NOT EXISTS idx_up_risk_profile ON user_profiles (risk_profile);

-- -----------------------------------------------------------------------------
-- 8. Compliance Records Table (ComplianceRecord)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS compliance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100) NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    consent_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    channel_type VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    consent_version VARCHAR(20),
    consent_given_at TIMESTAMP,
    consent_revoked_at TIMESTAMP,
    data_retention_until TIMESTAMP,
    deletion_requested_at TIMESTAMP,
    deletion_completed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_compliance_user_id ON compliance_records (user_id);
CREATE INDEX IF NOT EXISTS idx_compliance_type ON compliance_records (consent_type);
CREATE INDEX IF NOT EXISTS idx_compliance_status ON compliance_records (consent_status);
CREATE INDEX IF NOT EXISTS idx_compliance_user_type ON compliance_records (user_id, consent_type);

-- -----------------------------------------------------------------------------
-- 9. DND Registry Table (DndRegistry)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dnd_registry (
    phone_number VARCHAR(20) PRIMARY KEY,
    fully_blocked BOOLEAN DEFAULT FALSE,
    allowed_categories VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- 10. DND Audit Logs Table (DndAuditLog)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dnd_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100),
    phone_number VARCHAR(50),
    channel_type VARCHAR(20),
    event_type VARCHAR(100),
    action_taken VARCHAR(50),
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dnd_audit_phone ON dnd_audit_logs (phone_number);
CREATE INDEX IF NOT EXISTS idx_dnd_audit_user ON dnd_audit_logs (user_id);

-- -----------------------------------------------------------------------------
-- 11. Notification Analytics Table (NotificationAnalytics)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_type VARCHAR(20) NOT NULL,
    event_type VARCHAR(100),
    analytics_date DATE NOT NULL,
    total_sent BIGINT DEFAULT 0,
    total_delivered BIGINT DEFAULT 0,
    total_failed BIGINT DEFAULT 0,
    total_pending BIGINT DEFAULT 0,
    total_retried BIGINT DEFAULT 0,
    total_cancelled BIGINT DEFAULT 0,
    avg_delivery_time_ms DOUBLE PRECISION,
    p95_delivery_time_ms DOUBLE PRECISION,
    delivery_rate DOUBLE PRECISION,
    failure_rate DOUBLE PRECISION,
    total_read BIGINT DEFAULT 0,
    total_clicked BIGINT DEFAULT 0,
    total_opt_out BIGINT DEFAULT 0,
    total_cap_hits BIGINT DEFAULT 0,
    read_rate DOUBLE PRECISION,
    click_through_rate DOUBLE PRECISION,
    opt_out_rate DOUBLE PRECISION,
    estimated_cost_usd DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analytics_channel ON notification_analytics (channel_type);
CREATE INDEX IF NOT EXISTS idx_analytics_date ON notification_analytics (analytics_date);
CREATE INDEX IF NOT EXISTS idx_analytics_event_type ON notification_analytics (event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_channel_date ON notification_analytics (channel_type, analytics_date);

-- -----------------------------------------------------------------------------
-- 12. Audit Logs Table (AuditLog)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    user_id VARCHAR(100),
    actor_type VARCHAR(50),
    ip_address VARCHAR(45),
    before_state TEXT,
    after_state TEXT,
    description VARCHAR(1000),
    correlation_id VARCHAR(100),
    session_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_entity_type ON audit_logs (entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_entity_id ON audit_logs (entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs (created_at);

-- -----------------------------------------------------------------------------
-- 13. Notification Channels Table (NotificationChannel)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_type VARCHAR(20) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    simulation_mode BOOLEAN DEFAULT TRUE,
    provider_config TEXT,
    rate_limit_per_minute INT DEFAULT 1000,
    max_payload_size_bytes INT DEFAULT 65536,
    supports_html BOOLEAN DEFAULT FALSE,
    supports_attachments BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_channel_type ON notification_channels (channel_type);
CREATE INDEX IF NOT EXISTS idx_channel_active ON notification_channels (active);

-- -----------------------------------------------------------------------------
-- 14. Notification State Audit Table (NotificationStateAudit)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_state_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    previous_state VARCHAR(20),
    new_state VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_notification_id ON notification_state_audit (notification_id);

-- -----------------------------------------------------------------------------
-- 15. Scheduled Notifications Table (ScheduledNotification)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scheduled_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    template_id UUID,
    subject VARCHAR(500),
    body TEXT,
    template_variables TEXT,
    recipient_address VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    scheduled_at TIMESTAMP NOT NULL,
    next_run_at TIMESTAMP,
    last_run_at TIMESTAMP,
    cron_expression VARCHAR(100),
    recurrence_count INT DEFAULT 0,
    max_recurrences INT DEFAULT 1,
    notification_id UUID,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sched_user_id ON scheduled_notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_sched_status ON scheduled_notifications (status);
CREATE INDEX IF NOT EXISTS idx_sched_scheduled_at ON scheduled_notifications (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_sched_next_run ON scheduled_notifications (next_run_at);
