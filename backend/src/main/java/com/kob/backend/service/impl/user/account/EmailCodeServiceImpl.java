package com.kob.backend.service.impl.user.account;

import com.kob.backend.service.user.account.EmailCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class EmailCodeServiceImpl implements EmailCodeService {
    private static final long EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final long RESEND_INTERVAL_MILLIS = 60 * 1000L;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, CodeRecord> CODE_CACHE = new ConcurrentHashMap<>();

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Override
    public Map<String, String> sendCode(String email) {
        return sendCode(email, "REGISTER");
    }

    @Override
    public Map<String, String> sendCode(String email, String purpose) {
        Map<String, String> map = new HashMap<>();
        String targetEmail = normalizeEmail(email);
        String codePurpose = normalizePurpose(purpose);
        if (!isValidEmail(targetEmail)) {
            map.put("error_message", "邮箱格式不正确");
            return map;
        }
        if (from == null || from.trim().isEmpty()) {
            map.put("error_message", "邮件服务未配置，请设置 MAIL_USERNAME 和 MAIL_PASSWORD");
            return map;
        }

        cleanupExpired();
        long now = System.currentTimeMillis();
        String cacheKey = buildKey(targetEmail, codePurpose);
        CodeRecord oldRecord = CODE_CACHE.get(cacheKey);
        if (oldRecord != null && now - oldRecord.sendTime < RESEND_INTERVAL_MILLIS) {
            map.put("error_message", "验证码发送过于频繁，请稍后再试");
            return map;
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(targetEmail);
            message.setSubject(subjectOf(codePurpose));
            message.setText(textOf(codePurpose, code));
            mailSender.send(message);
        } catch (Exception e) {
            map.put("error_message", "邮件服务未配置或发送失败");
            return map;
        }

        CODE_CACHE.put(cacheKey, new CodeRecord(code, now));
        map.put("error_message", "success");
        return map;
    }

    @Override
    public boolean verifyAndConsume(String email, String code) {
        return verifyAndConsume(email, code, "REGISTER");
    }

    @Override
    public boolean verifyAndConsume(String email, String code, String purpose) {
        String targetEmail = normalizeEmail(email);
        String value = code == null ? "" : code.trim();
        String cacheKey = buildKey(targetEmail, normalizePurpose(purpose));
        CodeRecord record = CODE_CACHE.get(cacheKey);
        if (record == null || record.isExpired() || !record.code.equals(value)) {
            return false;
        }
        CODE_CACHE.remove(cacheKey);
        return true;
    }

    private void cleanupExpired() {
        CODE_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches() && email.length() <= 255;
    }

    private String normalizePurpose(String purpose) {
        String value = purpose == null ? "" : purpose.trim().toUpperCase();
        if ("LOGIN".equals(value) || "RESET_PASSWORD".equals(value) || "ACCOUNT_UPDATE".equals(value)) {
            return value;
        }
        return "REGISTER";
    }

    private String buildKey(String email, String purpose) {
        return purpose + ":" + email;
    }

    private String subjectOf(String purpose) {
        if ("LOGIN".equals(purpose)) return "校园论坛登录验证码";
        if ("RESET_PASSWORD".equals(purpose)) return "校园论坛找回密码验证码";
        if ("ACCOUNT_UPDATE".equals(purpose)) return "校园论坛账号修改验证码";
        return "校园论坛注册验证码";
    }

    private String textOf(String purpose, String code) {
        String action = "注册";
        if ("LOGIN".equals(purpose)) action = "登录";
        if ("RESET_PASSWORD".equals(purpose)) action = "找回密码";
        if ("ACCOUNT_UPDATE".equals(purpose)) action = "账号修改";
        return "你的校园论坛" + action + "验证码是：" + code + "，5 分钟内有效。若非本人操作，请忽略此邮件。";
    }

    private static class CodeRecord {
        private final String code;
        private final long sendTime;

        private CodeRecord(String code, long sendTime) {
            this.code = code;
            this.sendTime = sendTime;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - sendTime > EXPIRE_MILLIS;
        }
    }
}
