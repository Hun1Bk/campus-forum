package com.kob.backend.service.user.account;


import java.util.Map;

public interface LoginService {
    Map<String,String> getToken(String account, String email, String password);

    Map<String, String> sendEmailLoginCode(String email);

    Map<String, String> emailLogin(String email, String emailCode);

    Map<String, String> sendResetPasswordCode(String email);

    Map<String, String> resetPassword(String email, String emailCode, String password, String confirmedPassword);
}
