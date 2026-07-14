package com.dataart.ticketing.repository;

import java.util.Optional;
import java.util.UUID;

import com.dataart.ticketing.domain.EmailVerificationToken;
import com.dataart.ticketing.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    /** Invalidate any still-unused tokens for a user (issuing a new token supersedes them). */
    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = CURRENT_TIMESTAMP "
            + "where t.user = :user and t.usedAt is null")
    void invalidateActiveTokens(@Param("user") User user);
}
