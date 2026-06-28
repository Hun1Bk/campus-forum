package com.kob.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminBootstrap implements ApplicationRunner {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("account", "VARCHAR(100) DEFAULT NULL");
        addColumnIfMissing("email", "VARCHAR(255) DEFAULT NULL");
        addUserIndexIfMissing("uk_user_email", "CREATE UNIQUE INDEX uk_user_email ON user(email)");
        addUserIndexIfMissing("uk_user_account", "CREATE UNIQUE INDEX uk_user_account ON user(account)");
        addColumnIfMissing("role", "VARCHAR(20) NOT NULL DEFAULT 'USER'");
        addColumnIfMissing("status", "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
        addColumnIfMissing("custom_title", "VARCHAR(100) NOT NULL DEFAULT ''");
        addColumnIfMissing("account_update_time", "VARCHAR(100)");
        ensureSectionTable();
        ensureBlockRuleTable();
        ensurePostViewRecordTable();
        ensureDefaultSection();
        addCommentColumnIfMissing("parent_id", "INT DEFAULT NULL");
        addPostColumnIfMissing("image_urls", "TEXT");
        addPostColumnIfMissing("section_id", "INT NOT NULL DEFAULT 1");
        addPostColumnIfMissing("view_count", "INT NOT NULL DEFAULT 1");
        addPostColumnIfMissing("is_top", "TINYINT(1) NOT NULL DEFAULT 0");
        addPostColumnIfMissing("top_time", "VARCHAR(100)");
        addPostColumnIfMissing("title", "VARCHAR(200) NOT NULL DEFAULT ''");
        addPostColumnIfMissing("visibility", "VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'");
        jdbcTemplate.update("UPDATE user SET account = username WHERE account IS NULL OR account = ''");
        jdbcTemplate.update("UPDATE user SET role = 'USER' WHERE role IS NULL OR role = ''");
        jdbcTemplate.update("UPDATE user SET status = 'ACTIVE' WHERE status IS NULL OR status = ''");
        jdbcTemplate.update("UPDATE user SET custom_title = '' WHERE custom_title IS NULL");
        jdbcTemplate.update("UPDATE post SET section_id = 1 WHERE section_id IS NULL OR section_id <= 0");
        jdbcTemplate.update("UPDATE post SET view_count = 1 WHERE view_count IS NULL OR view_count <= 0");
        jdbcTemplate.update("UPDATE post SET is_top = 0 WHERE is_top IS NULL");
        jdbcTemplate.update("UPDATE post SET top_time = NULL WHERE is_top = 0");
        jdbcTemplate.update("UPDATE post SET title = LEFT(COALESCE(content, ''), 30) WHERE title IS NULL OR title = ''");
        jdbcTemplate.update("UPDATE post SET visibility = 'PUBLIC' WHERE visibility IS NULL OR visibility = ''");
        ensureAdminUser();
    }

    private void addColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE user ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void addPostColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE post ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void addCommentColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE comment ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void ensureSectionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS section (" +
                "sid INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL UNIQUE, " +
                "description VARCHAR(255), " +
                "sort_order INT DEFAULT 0, " +
                "create_time VARCHAR(100)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }

    private void ensureBlockRuleTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS block_rule (" +
                "bid INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "target_type VARCHAR(20) NOT NULL, " +
                "target_id INT NOT NULL, " +
                "create_time VARCHAR(100), " +
                "UNIQUE KEY uk_block_user_target (user_id, target_type, target_id), " +
                "INDEX idx_block_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }

    private void ensurePostViewRecordTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS post_view_record (" +
                "vid INT AUTO_INCREMENT PRIMARY KEY, " +
                "pid INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "last_view_time VARCHAR(100), " +
                "UNIQUE KEY uk_post_view_user (pid, user_id), " +
                "INDEX idx_post_view_pid (pid), " +
                "INDEX idx_post_view_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }

    private void ensureDefaultSection() {
        List<Integer> ids = jdbcTemplate.query(
                "SELECT sid FROM section WHERE sid = 1",
                (rs, rowNum) -> rs.getInt("sid")
        );
        if (!ids.isEmpty()) {
            jdbcTemplate.update("UPDATE section SET name = '综合讨论' WHERE sid = 1");
            return;
        }
        List<Integer> defaultNameIds = jdbcTemplate.query(
                "SELECT sid FROM section WHERE name = '综合讨论'",
                (rs, rowNum) -> rs.getInt("sid")
        );
        if (defaultNameIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO section(sid, name, description, sort_order, create_time) VALUES (1, '综合讨论', '默认论坛分区', 1, NOW())"
            );
        } else {
            jdbcTemplate.update("UPDATE section SET sid = 1, description = '默认论坛分区', sort_order = 1 WHERE sid = ?", defaultNameIds.get(0));
        }
    }

    private void ensureAdminUser() {
        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            adminPassword = "admin123456";
        }
        List<Integer> ids = jdbcTemplate.query(
                "SELECT id FROM user WHERE username = 'admin'",
                (rs, rowNum) -> rs.getInt("id")
        );
        if (ids.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO user(username, account, email, password, photo, role, status, custom_title) VALUES (?, ?, NULL, ?, ?, 'OWNER', 'ACTIVE', '')",
                    "admin",
                    "admin",
                    passwordEncoder.encode(adminPassword),
                    "https://pic2.zhimg.com/80/v2-bed439ff53b5cc854445e58c85e37c45_1440w.webp"
            );
        } else {
            jdbcTemplate.update("UPDATE user SET account = 'admin', role = 'OWNER', status = 'ACTIVE' WHERE username = 'admin'");
        }
    }

    private void addUserIndexIfMissing(String indexName, String statement) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND INDEX_NAME = ?",
                Integer.class,
                indexName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(statement);
        }
    }
}
