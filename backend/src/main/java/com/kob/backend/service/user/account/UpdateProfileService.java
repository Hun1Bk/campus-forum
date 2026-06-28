package com.kob.backend.service.user.account;

import java.util.Map;

public interface UpdateProfileService {
    Map<String, String> update(String username, String photo, String password, String confirmedPassword);
}
