package com.disasterrescue.api.repository;

import com.disasterrescue.api.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordResetToken p SET p.usedAt = :usedAt WHERE p.user.id = :userId AND p.usedAt IS NULL")
    void invalidateAllByUserId(@Param("userId") Long userId, @Param("usedAt") Instant usedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt <= :now OR p.usedAt IS NOT NULL")
    int deleteExpiredOrUsed(@Param("now") Instant now);
}
