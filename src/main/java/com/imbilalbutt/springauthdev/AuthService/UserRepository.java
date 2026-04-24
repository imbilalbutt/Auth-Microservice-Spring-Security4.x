package com.imbilalbutt.springauthdev.AuthService;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.emailVerified = true, u.emailVerificationToken = NULL WHERE u.emailVerificationToken = :token")
    void verifyEmailByToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.passwordResetToken = NULL, u.passwordResetTokenExpiry = NULL WHERE u.id = :userId")
    void clearPasswordResetToken(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts, u.locked = true, u.accountLockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") Integer userId, @Param("attempts") int attempts, @Param("lockedUntil") LocalDateTime lockedUntil);
}