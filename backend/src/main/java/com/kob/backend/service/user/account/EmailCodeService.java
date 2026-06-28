package com.kob.backend.service.user.account;

import java.util.Map;

public interface EmailCodeService {
    Map<String, String> sendCode(String email);

    boolean verifyAndConsume(String email, String code);
}
