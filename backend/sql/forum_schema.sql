CREATE DATABASE IF NOT EXISTS kob DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kob;

CREATE TABLE IF NOT EXISTS user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  account VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) DEFAULT NULL,
  password VARCHAR(255) NOT NULL,
  photo VARCHAR(1024),
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  custom_title VARCHAR(100) NOT NULL DEFAULT '',
  account_update_time VARCHAR(100),
  UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post (
  pid INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL DEFAULT '',
  content TEXT,
  timer VARCHAR(100),
  id INT NOT NULL,
  seen TINYINT(1) DEFAULT 1,
  image_urls TEXT,
  section_id INT NOT NULL DEFAULT 1,
  view_count INT NOT NULL DEFAULT 1,
  is_top TINYINT(1) NOT NULL DEFAULT 0,
  top_time VARCHAR(100),
  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
  INDEX idx_post_user (id),
  INDEX idx_post_section (section_id),
  INDEX idx_post_top (is_top, top_time),
  INDEX idx_post_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS section (
  sid INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(255),
  sort_order INT DEFAULT 0,
  create_time VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO section(sid, name, description, sort_order, create_time)
VALUES (1, '综合讨论', '默认论坛分区', 1, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

CREATE TABLE IF NOT EXISTS comment (
  cid INT AUTO_INCREMENT PRIMARY KEY,
  content TEXT,
  pid INT NOT NULL,
  id INT NOT NULL,
  parent_id INT DEFAULT NULL,
  INDEX idx_comment_post (pid),
  INDEX idx_comment_user (id),
  INDEX idx_comment_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agree (
  aid INT AUTO_INCREMENT PRIMARY KEY,
  pid INT NOT NULL,
  id INT NOT NULL,
  UNIQUE KEY uk_agree_post_user (pid, id),
  INDEX idx_agree_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification (
  nid INT AUTO_INCREMENT PRIMARY KEY,
  receiver_id INT NOT NULL,
  actor_id INT NOT NULL,
  pid INT NOT NULL,
  type VARCHAR(30) NOT NULL,
  content VARCHAR(500),
  is_read TINYINT(1) DEFAULT 0,
  create_time VARCHAR(100),
  INDEX idx_notification_receiver (receiver_id, is_read, nid),
  INDEX idx_notification_post (pid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS block_rule (
  bid INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id INT NOT NULL,
  create_time VARCHAR(100),
  UNIQUE KEY uk_block_user_target (user_id, target_type, target_id),
  INDEX idx_block_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_view_record (
  vid INT AUTO_INCREMENT PRIMARY KEY,
  pid INT NOT NULL,
  user_id INT NOT NULL,
  last_view_time VARCHAR(100),
  UNIQUE KEY uk_post_view_user (pid, user_id),
  INDEX idx_post_view_pid (pid),
  INDEX idx_post_view_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
