package com.disasterrescue.api.service.impl;

import com.disasterrescue.api.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final Environment environment;

    public MailServiceImpl(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink, String rawToken) {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (isProduction) {
            // Production: Không ghi token ra log để đảm bảo an toàn nếu chưa tích hợp SMTP
            log.info("[PRODUCTION EMAIL] Yêu cầu đặt lại mật khẩu đã được xử lý cho email: {}", toEmail);
        } else {
            // Dev environment: Ghi log link chứa token để lập trình viên và kiểm thử viên sử dụng trực tiếp
            log.info("================================================================================");
            log.info("[DEV EMAIL SIMULATION] Đặt lại mật khẩu cho tài khoản: {}", toEmail);
            log.info("Link đặt lại mật khẩu: {}", resetLink);
            log.info("Token: {}", rawToken);
            log.info("================================================================================");
        }
    }
}
