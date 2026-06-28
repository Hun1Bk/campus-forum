package com.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String account;
    private String email;
    private String password;
    private String photo;
    private String role;
    private String status;
    private String customTitle;
    private String accountUpdateTime;

    public User() {
    }

    public User(Integer id, String username, String account, String email, String password, String photo, String role, String status, String customTitle, String accountUpdateTime) {
        this.id = id;
        this.username = username;
        this.account = account;
        this.email = email;
        this.password = password;
        this.photo = photo;
        this.role = role;
        this.status = status;
        this.customTitle = customTitle;
        this.accountUpdateTime = accountUpdateTime;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomTitle() { return customTitle; }
    public void setCustomTitle(String customTitle) { this.customTitle = customTitle; }
    public String getAccountUpdateTime() { return accountUpdateTime; }
    public void setAccountUpdateTime(String accountUpdateTime) { this.accountUpdateTime = accountUpdateTime; }
}
