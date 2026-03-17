-- PhishGuard AI - MySQL schema
-- Matches JPA entity: com.phishguardai.backend.entity.AnalysisRecord

CREATE TABLE IF NOT EXISTS analysis_records (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL,
  input_text TEXT NULL,
  input_url VARCHAR(2048) NULL,
  input_file_name VARCHAR(255) NULL,
  input_file_type VARCHAR(255) NULL,
  input_file_size_bytes BIGINT NULL,
  verdict VARCHAR(16) NOT NULL,
  risk_score INT NOT NULL,
  severity VARCHAR(16) NOT NULL,
  confidence DOUBLE NOT NULL,
  ai_summary TEXT NULL,
  safe_browsing_flagged BOOLEAN NOT NULL DEFAULT FALSE,
  threat_types TEXT NULL,
  red_flags_json LONGTEXT NULL,
  actions_json LONGTEXT NULL,
  sandbox_scan_json LONGTEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_analysis_user_created (user_id, created_at),
  INDEX idx_analysis_user (user_id)
);

CREATE TABLE IF NOT EXISTS incoming_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_message_id VARCHAR(255) NULL,
  sender_label VARCHAR(180) NULL,
  sender_address VARCHAR(255) NULL,
  subject_line VARCHAR(240) NULL,
  preview_text TEXT NULL,
  body_text TEXT NULL,
  body_html LONGTEXT NULL,
  received_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP NULL,
  analysis_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_incoming_event_message (user_id, source_type, source_message_id),
  INDEX idx_incoming_user_created (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS extracted_urls (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  incoming_event_id BIGINT NOT NULL,
  original_url VARCHAR(2048) NOT NULL,
  normalized_url VARCHAR(2048) NULL,
  final_url VARCHAR(2048) NULL,
  valid_url BOOLEAN NOT NULL DEFAULT FALSE,
  shortened BOOLEAN NOT NULL DEFAULT FALSE,
  redirected BOOLEAN NOT NULL DEFAULT FALSE,
  spoofed BOOLEAN NOT NULL DEFAULT FALSE,
  suspicious BOOLEAN NOT NULL DEFAULT FALSE,
  malicious BOOLEAN NOT NULL DEFAULT FALSE,
  safe_browsing_flagged BOOLEAN NOT NULL DEFAULT FALSE,
  risk_score INT NOT NULL,
  status_label VARCHAR(32) NOT NULL,
  evidence_json TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_extracted_urls_event (incoming_event_id)
);

CREATE TABLE IF NOT EXISTS security_alerts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL,
  analysis_id BIGINT NOT NULL,
  incoming_event_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_message_id VARCHAR(255) NULL,
  sender_label VARCHAR(180) NULL,
  subject_line VARCHAR(240) NULL,
  preview_text TEXT NULL,
  file_name VARCHAR(255) NULL,
  verdict VARCHAR(16) NOT NULL,
  risk_score INT NOT NULL,
  confidence_score DOUBLE NOT NULL DEFAULT 0,
  severity VARCHAR(16) NOT NULL,
  safe_to_open BOOLEAN NOT NULL DEFAULT FALSE,
  short_explanation TEXT NULL,
  url_status_summary VARCHAR(255) NULL,
  popup_title VARCHAR(160) NOT NULL,
  popup_message TEXT NULL,
  read_status BOOLEAN NOT NULL DEFAULT FALSE,
  opened_anyway BOOLEAN NOT NULL DEFAULT FALSE,
  cancelled BOOLEAN NOT NULL DEFAULT FALSE,
  blocked_sender BOOLEAN NOT NULL DEFAULT FALSE,
  reported_phishing BOOLEAN NOT NULL DEFAULT FALSE,
  primary_target_url VARCHAR(2048) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_security_alert_user_created (user_id, created_at),
  INDEX idx_security_alert_user_read (user_id, read_status)
);

CREATE TABLE IF NOT EXISTS blocked_senders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  sender_key VARCHAR(255) NOT NULL,
  reason VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_blocked_sender (user_id, source_type, sender_key),
  INDEX idx_blocked_sender_user (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS phishing_reports (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL,
  alert_id BIGINT NOT NULL,
  analysis_id BIGINT NOT NULL,
  report_note TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_phishing_reports_user (user_id, created_at)
);

