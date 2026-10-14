package com.disasterrescue.api.service;

public interface MailService {
    void sendPasswordResetEmail(String toEmail, String resetLink, String rawToken);
}
