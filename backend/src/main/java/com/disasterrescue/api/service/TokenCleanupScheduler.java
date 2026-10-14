package com.disasterrescue.api.service;

import com.disasterrescue.api.repository.PasswordResetTokenRepository;
import com.disasterrescue.api.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public TokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository,
                                 PasswordResetTokenRepository passwordResetTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /**
     * Chạy định kỳ lúc 02:00 sáng hàng ngày để dọn dẹp các token đã hết hạn hoặc bị thu hồi.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int deletedRefreshTokens = refreshTokenRepository.deleteExpiredOrRevoked(now);
        int deletedResetTokens = passwordResetTokenRepository.deleteExpiredOrUsed(now);

        log.info("Đã hoàn tất dọn dẹp token định kỳ: {} refresh tokens và {} reset tokens đã được xóa.",
                deletedRefreshTokens, deletedResetTokens);
    }
}
