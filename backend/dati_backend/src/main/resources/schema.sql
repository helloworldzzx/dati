CREATE DATABASE IF NOT EXISTS dati_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE dati_db;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  phone VARCHAR(20) NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(64) NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  must_change_password TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_phone (phone),
  KEY idx_sys_user_role_status (role, status),
  CONSTRAINT chk_sys_user_role CHECK (role IN ('ADMIN', 'USER')),
  CONSTRAINT chk_sys_user_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_category (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NULL,
  name VARCHAR(100) NOT NULL,
  level TINYINT UNSIGNED NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_question_category_parent (parent_id),
  KEY idx_question_category_level_status (level, status),
  UNIQUE KEY uk_question_category_parent_name (parent_id, name),
  CONSTRAINT fk_question_category_parent
    FOREIGN KEY (parent_id) REFERENCES question_category (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_question_category_level CHECK (level BETWEEN 1 AND 3),
  CONSTRAINT chk_question_category_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NOT NULL,
  type VARCHAR(20) NOT NULL,
  title TEXT NOT NULL,
  correct_answer TEXT NULL,
  analysis TEXT NULL,
  source_file VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  answer_count INT UNSIGNED NOT NULL DEFAULT 0,
  correct_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_question_category_type_status (category_id, type, status),
  KEY idx_question_created_by (created_by),
  CONSTRAINT fk_question_category
    FOREIGN KEY (category_id) REFERENCES question_category (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_question_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user (id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_question_type CHECK (type IN ('SINGLE', 'MULTIPLE', 'JUDGE', 'ANALYSIS')),
  CONSTRAINT chk_question_status CHECK (status IN ('DRAFT', 'ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_option (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  option_key VARCHAR(10) NOT NULL,
  option_content TEXT NOT NULL,
  is_correct TINYINT(1) NOT NULL DEFAULT 0,
  sort_no INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_option_key (question_id, option_key),
  KEY idx_question_option_question_sort (question_id, sort_no),
  CONSTRAINT fk_question_option_question
    FOREIGN KEY (question_id) REFERENCES question (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS practice_session (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NULL,
  mode VARCHAR(20) NOT NULL DEFAULT 'PRACTICE',
  total_count INT UNSIGNED NOT NULL DEFAULT 0,
  answered_count INT UNSIGNED NOT NULL DEFAULT 0,
  correct_count INT UNSIGNED NOT NULL DEFAULT 0,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_practice_session_user_started (user_id, started_at),
  KEY idx_practice_session_category (category_id),
  CONSTRAINT fk_practice_session_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_practice_session_category
    FOREIGN KEY (category_id) REFERENCES question_category (id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_practice_session_mode CHECK (mode IN ('PRACTICE', 'WRONG_BOOK', 'FAVORITE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS answer_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  user_answer TEXT NULL,
  is_correct TINYINT(1) NULL,
  duration_seconds INT UNSIGNED NOT NULL DEFAULT 0,
  answered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_answer_record_session (session_id),
  KEY idx_answer_record_user_question (user_id, question_id),
  KEY idx_answer_record_question (question_id),
  KEY idx_answer_record_answered_at (answered_at),
  CONSTRAINT fk_answer_record_session
    FOREIGN KEY (session_id) REFERENCES practice_session (id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_answer_record_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_answer_record_question
    FOREIGN KEY (question_id) REFERENCES question (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_question_stat (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  answer_count INT UNSIGNED NOT NULL DEFAULT 0,
  correct_count INT UNSIGNED NOT NULL DEFAULT 0,
  wrong_count INT UNSIGNED NOT NULL DEFAULT 0,
  last_answer TEXT NULL,
  last_is_correct TINYINT(1) NULL,
  is_favorite TINYINT(1) NOT NULL DEFAULT 0,
  favorite_at DATETIME NULL,
  last_answered_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_question_stat_user_question (user_id, question_id),
  KEY idx_user_question_stat_wrong (user_id, wrong_count),
  KEY idx_user_question_stat_favorite (user_id, is_favorite),
  CONSTRAINT fk_user_question_stat_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_user_question_stat_question
    FOREIGN KEY (question_id) REFERENCES question (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_answer_stat (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  answer_count INT UNSIGNED NOT NULL DEFAULT 0,
  correct_count INT UNSIGNED NOT NULL DEFAULT 0,
  wrong_count INT UNSIGNED NOT NULL DEFAULT 0,
  accuracy_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  total_duration_seconds BIGINT UNSIGNED NOT NULL DEFAULT 0,
  last_answered_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_answer_stat_user (user_id),
  KEY idx_user_answer_stat_rank (correct_count DESC, answer_count DESC, accuracy_rate DESC, last_answered_at),
  CONSTRAINT fk_user_answer_stat_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_import_batch (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  total_count INT UNSIGNED NOT NULL DEFAULT 0,
  success_count INT UNSIGNED NOT NULL DEFAULT 0,
  fail_count INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
  imported_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_question_import_batch_imported_by (imported_by),
  KEY idx_question_import_batch_status (status),
  CONSTRAINT fk_question_import_batch_imported_by
    FOREIGN KEY (imported_by) REFERENCES sys_user (id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_question_import_batch_status CHECK (status IN ('PROCESSING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_import_error (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_id BIGINT UNSIGNED NOT NULL,
  row_no INT UNSIGNED NOT NULL,
  error_message VARCHAR(500) NOT NULL,
  raw_data JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_question_import_error_batch (batch_id),
  CONSTRAINT fk_question_import_error_batch
    FOREIGN KEY (batch_id) REFERENCES question_import_batch (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
