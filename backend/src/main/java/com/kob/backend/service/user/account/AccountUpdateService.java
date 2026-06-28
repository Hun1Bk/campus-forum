package com.kob.backend.service.user.account;

import java.util.Map;

public interface AccountUpdateService {
    Map<String, String> sendCode();

    Map<String, String> update(String account, String emailCode);
}
