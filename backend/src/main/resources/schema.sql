-- =========================================================
-- AquaNexus — Smart Water Reuse Analysis & CTO Monitoring
-- SQL Database Schema (Compatible with MySQL 8.0 & H2 Database)
-- =========================================================

CREATE DATABASE IF NOT EXISTS aquanexus;
USE aquanexus;

-- 1. Users Table (Authentication & User Management)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NULL,
    password VARCHAR(255) NULL,
    photo_url VARCHAR(500) NULL,
    role VARCHAR(50) DEFAULT 'Industry Admin',
    notification_enabled BOOLEAN DEFAULT TRUE,
    email_notifications_enabled BOOLEAN DEFAULT TRUE,
    sms_notifications_enabled BOOLEAN DEFAULT TRUE,
    is_oauth BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Water Daily Records Table (Usage Monitoring & ML Features)
CREATE TABLE IF NOT EXISTS water_daily_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    industry_id INT NOT NULL,
    industry_name VARCHAR(200) NULL,
    cto_number VARCHAR(100) NULL,
    facility_location VARCHAR(255) NULL,
    plant_id VARCHAR(100) NULL,
    industry_type VARCHAR(100) NULL,
    responsible_officer VARCHAR(100) NULL,
    contact_info VARCHAR(200) NULL,
    department VARCHAR(100) DEFAULT 'Production',
    date DATE NOT NULL,
    fresh_water_consumed DOUBLE NOT NULL,
    wastewater_generated DOUBLE NOT NULL,
    reused_water DOUBLE NOT NULL,
    cto_limit DOUBLE NOT NULL,
    reuse_percentage DOUBLE NULL,
    cto_utilization DOUBLE NULL,
    water_loss DOUBLE NULL,
    compliance_status VARCHAR(20) NULL,
    is_anomaly BOOLEAN DEFAULT FALSE,
    treated_water_available DOUBLE NULL,
    main_meter_consumption DOUBLE NULL,
    total_department_consumption DOUBLE NULL,
    opening_meter_reading DOUBLE NULL,
    closing_meter_reading DOUBLE NULL,
    unaccounted_water DOUBLE NULL,
    reuse_recovery_rate DOUBLE NULL,
    meter_record_type VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexing for fast time-series queries
CREATE INDEX IF NOT EXISTS idx_industry_date ON water_daily_records(industry_id, date);

-- 3. Water Alerts Table (Compliance, CTO & Usage Alerts)
CREATE TABLE IF NOT EXISTS water_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    industry_id INT NOT NULL,
    department VARCHAR(100) NULL,
    record_id BIGINT NULL,
    alert_date DATE NOT NULL,
    alert_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NULL,
    message VARCHAR(500) NOT NULL,
    explanation VARCHAR(1000) NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_by VARCHAR(100) NULL,
    resolved_at TIMESTAMP NULL,
    resolution_notes VARCHAR(1000) NULL,
    notification_sent BOOLEAN DEFAULT FALSE,
    email_sent BOOLEAN DEFAULT FALSE,
    sms_sent BOOLEAN DEFAULT FALSE,
    last_notification_sent_at TIMESTAMP NULL,
    notification_failure_reason VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alerts_industry_date ON water_alerts(industry_id, alert_date);

-- 4. Notification Settings Table (Facility preferences & thresholds)
CREATE TABLE IF NOT EXISTS notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    industry_id INT NOT NULL UNIQUE,
    industry_name VARCHAR(200) DEFAULT 'Demo Dairy Industry',
    registered_email VARCHAR(255) DEFAULT 'compliance@aquanexus.ind',
    registered_mobile VARCHAR(20) DEFAULT '+919876543210',
    email_notifications_enabled BOOLEAN DEFAULT TRUE,
    sms_notifications_enabled BOOLEAN DEFAULT TRUE,
    notify_warning BOOLEAN DEFAULT TRUE,
    notify_critical BOOLEAN DEFAULT TRUE,
    notify_exceeded BOOLEAN DEFAULT TRUE,
    notify_abnormal_usage BOOLEAN DEFAULT TRUE,
    recipient_roles VARCHAR(500) DEFAULT 'Industry Admin,Compliance Officer,Plant Manager',
    monthly_report_email_enabled BOOLEAN DEFAULT TRUE,
    unaccounted_water_threshold_pct DOUBLE DEFAULT 3.0,
    unaccounted_water_threshold_kl DOUBLE DEFAULT 5.0,
    low_reuse_target_pct DOUBLE DEFAULT 60.0,
    untapped_reuse_threshold_kl DOUBLE DEFAULT 15.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 5. Notification Logs Table (Audit trail of Email & SMS deliveries)
CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NULL,
    recipient_user_id BIGINT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient_address VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NULL,
    message VARCHAR(2000) NULL,
    delivery_status VARCHAR(30) NOT NULL,
    provider_response VARCHAR(1000) NULL,
    errorMessage VARCHAR(1000) NULL,
    attempt_count INT DEFAULT 1,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_alert ON notification_logs(alert_id);
