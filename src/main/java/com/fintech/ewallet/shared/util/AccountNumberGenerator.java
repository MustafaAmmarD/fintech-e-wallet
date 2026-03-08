package com.fintech.ewallet.shared.util;

import java.security.SecureRandom;

/**
 * Generates and validates Luhn-checked account numbers.
 * <p>
 * Format: 9 digits total (8 random digits + 1 Luhn check digit).
 * Example: "192967789"
 */
public final class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BASE_LENGTH = 8; // 8 random digits + 1 check digit = 9 total

    private AccountNumberGenerator() {
        // Utility class
    }

    /**
     * Generate a new Luhn-valid 9-digit account number.
     */
    public static String generate() {
        // Generate 8 random digits (first digit 1-9 to avoid leading zero)
        StringBuilder sb = new StringBuilder(BASE_LENGTH);
        sb.append(1 + RANDOM.nextInt(9)); // First digit: 1-9
        for (int i = 1; i < BASE_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10)); // Remaining digits: 0-9
        }

        String baseNumber = sb.toString();
        int checkDigit = calculateLuhnCheckDigit(baseNumber);
        return baseNumber + checkDigit;
    }

    /**
     * Validate that an account number passes the Luhn check.
     */
    public static boolean isValid(String accountNumber) {
        if (accountNumber == null || accountNumber.length() != 9) {
            return false;
        }
        // Check all characters are digits
        for (char c : accountNumber.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return luhnCheck(accountNumber);
    }

    /**
     * Calculate the Luhn check digit for a given number string.
     */
    private static int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        // Process from right to left, doubling every other digit
        // Since we're appending a check digit, the existing digits shift
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if ((number.length() - i) % 2 == 1) {
                // Odd position from right (after adding check digit) — double it
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Verify the full number (including check digit) using Luhn algorithm.
     */
    private static boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
