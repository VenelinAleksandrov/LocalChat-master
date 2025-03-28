package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_SESSIONS")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String sessionId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;

    @Column(name = "USER_ID")
    private Long userId;


    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public LocalDateTime getLogoutTime() { return logoutTime; }
    public Long getUserId() { return userId; }
    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }
}