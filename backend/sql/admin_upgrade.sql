USE kob;

ALTER TABLE user ADD COLUMN email VARCHAR(255) DEFAULT NULL;
ALTER TABLE user ADD COLUMN account VARCHAR(100) DEFAULT NULL;
ALTER TABLE user ADD COLUMN account_update_time VARCHAR(100);
CREATE UNIQUE INDEX uk_user_email ON user(email);
CREATE UNIQUE INDEX uk_user_account ON user(account);
ALTER TABLE user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE user ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE user ADD COLUMN custom_title VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE post ADD COLUMN is_top TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE post ADD COLUMN top_time VARCHAR(100);
ALTER TABLE post ADD COLUMN title VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE post ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE comment ADD COLUMN parent_id INT DEFAULT NULL;

CREATE TABLE IF NOT EXISTS block_rule (
  bid INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id INT NOT NULL,
  create_time VARCHAR(100),
  UNIQUE KEY uk_block_user_target (user_id, target_type, target_id),
  INDEX idx_block_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE user SET account = username WHERE account IS NULL OR account = '';
UPDATE user SET role = 'USER' WHERE role IS NULL OR role = '';
UPDATE user SET status = 'ACTIVE' WHERE status IS NULL OR status = '';
UPDATE user SET custom_title = '' WHERE custom_title IS NULL;
UPDATE post SET is_top = 0 WHERE is_top IS NULL;
UPDATE post SET title = LEFT(COALESCE(content, ''), 30) WHERE title IS NULL OR title = '';
UPDATE post SET visibility = 'PUBLIC' WHERE visibility IS NULL OR visibility = '';

-- The Spring Boot startup initializer creates or repairs the admin account.
-- Set ADMIN_PASSWORD in the environment before first startup to choose the password.
-- with a BCrypt password hash.
