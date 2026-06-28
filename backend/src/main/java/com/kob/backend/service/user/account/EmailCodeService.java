package com.kob.backend.service.user.account;

import java.util.Map;

public interface EmailCodeService {
    Map<String, String> sendCode(String email);

    Map<String, String> sendCode(String email, String purpose);

    boolean verifyAndConsume(String email, String code);

    boolean verifyAndConsume(String email, String code, String purpose);
}
