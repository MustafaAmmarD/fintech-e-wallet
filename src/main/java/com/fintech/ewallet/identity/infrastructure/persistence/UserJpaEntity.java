package com.fintech.ewallet.identity.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the users table.
 * Maps to the database — separate from the domain {@code User} entity.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "kyc_status", nullable = false, length = 20)
    private String kycStatus;

    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "language", nullable = false, length = 5)
    private String language;

    @Column(name = "referral_code", unique = true, length = 20)
    private String referralCode;

    @Column(name = "account_number", unique = true, length = 15)
    private String accountNumber;

    @Column(name = "show_full_name", nullable = false)
    private boolean showFullName;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = Instant.now();
        if (updatedAt == null)
            updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
