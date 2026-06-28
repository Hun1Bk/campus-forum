package com.kob.backend.service.user.account;

import java.util.Map;

public interface RegisterService {
    Map<String,String> Register(String username, String account, String email, String password, String confirmedPassword, String emailCode);
}
