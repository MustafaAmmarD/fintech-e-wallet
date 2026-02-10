package com.fintech.ewallet.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * User domain entity — pure Java POJO.
 * <p>
 * No JPA, no Spring, no Lombok. Business logic lives here.
 * JPA persistence is handled by {@code UserJpaEntity} in the infrastructure
 * layer.
 */
public class User {

    private UUID id;
    private String phoneNumber; // E.164 format: +967XXXXXXXXX
    private String fullName;
    private String passwordHash; // BCrypt hash — never plain text
    private String email; // Optional
    private KycStatus kycStatus;
    private AccountStatus accountStatus;
    private String language; // "ar" or "en"
    private String referralCode; // Unique code for referral program
    private int failedLoginAttempts;
    private Instant lastLoginAt;
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt; // Soft delete — null means active

    // ─── Constructors ────────────────────────────────────────

    public User() {
        // Default constructor
    }

    /**
     * Factory method for creating a new user during registration.
     */
    public static User createNew(String phoneNumber, String fullName,
            String passwordHash, String email,
            String language, String referralCode) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.phoneNumber = phoneNumber;
        user.fullName = fullName;
        user.passwordHash = passwordHash;
        user.email = email;
        user.kycStatus = KycStatus.NONE;
        user.accountStatus = AccountStatus.ACTIVE;
        user.language = (language != null) ? language : "ar";
        user.referralCode = referralCode;
        user.failedLoginAttempts = 0;
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        return user;
    }

    // ─── Business Logic ──────────────────────────────────────

    /**
     * Check if the account is currently active and not locked.
     */
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE && deletedAt == null;
    }

    /**
     * Check if the account is currently locked due to failed login attempts.
     */
    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    /**
     * Record a failed login attempt. Locks account after 5 failures.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plusSeconds(30 * 60); // Lock for 30 minutes
        }
    }

    /**
     * Reset failed login attempts after successful login.
     */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ─── Getters ─────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public String getLanguage() {
        return language;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    // ─── Setters ─────────────────────────────────────────────

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
