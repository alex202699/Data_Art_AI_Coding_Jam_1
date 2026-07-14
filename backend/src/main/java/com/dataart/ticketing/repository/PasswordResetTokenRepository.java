package com.dataart.ticketing.repository;

import java.util.Optional;
import java.util.UUID;

import com.dataart.ticketing.domain.PasswordResetToken;
import com.dataart.ticketing.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Invalidate any still-unused reset tokens for a user (a new request supersedes them). */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = CURRENT_TIMESTAMP "
            + "where t.user = :user and t.usedAt is null")
    void invalidateActiveTokens(@Param("user") User user);
}
