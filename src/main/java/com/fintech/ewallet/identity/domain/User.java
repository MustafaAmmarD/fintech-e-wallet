package com.fintech.ewallet.identity.domain;

import com.fintech.ewallet.shared.util.AccountNumberGenerator;

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
    private String englishFullName;
    private String gender;
    private String dateOfBirth;
    private String idNumber;
    private String maritalStatus;
    private KycStatus kycStatus;
    private AccountStatus accountStatus;
    private UserRole role;
    private String language; // "ar" or "en"
    private String referralCode; // Unique code for referral program
    private String accountNumber; // Luhn-validated 9-digit account number
    private boolean showFullName;
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
            String language, String referralCode,
            String englishFullName, String gender, String dateOfBirth, String idNumber, String maritalStatus) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.phoneNumber = phoneNumber;
        user.fullName = fullName;
        user.passwordHash = passwordHash;
        user.email = email;
        user.englishFullName = englishFullName;
        user.gender = gender;
        user.dateOfBirth = dateOfBirth;
        user.idNumber = idNumber;
        user.maritalStatus = maritalStatus;
        user.kycStatus = KycStatus.NONE;
        user.accountStatus = AccountStatus.ACTIVE;
        user.role = UserRole.USER;
        user.language = (language != null) ? language : "ar";
        user.referralCode = referralCode;
        user.accountNumber = AccountNumberGenerator.generate();
        user.showFullName = true;
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

    /**
     * Update KYC status (e.g., when documents are uploaded or reviewed).
     */
    public void updateKycStatus(KycStatus newStatus) {
        this.kycStatus = newStatus;
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

    public UserRole getRole() {
        return role;
    }

    public String getLanguage() {
        return language;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public boolean isShowFullName() {
        return showFullName;
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

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setShowFullName(boolean showFullName) {
        this.showFullName = showFullName;
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

    public String getEnglishFullName() { return englishFullName; }
    public void setEnglishFullName(String englishFullName) { this.englishFullName = englishFullName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
}
