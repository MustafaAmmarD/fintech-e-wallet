package com.fintech.ewallet.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountNumberGenerator.
 *
 * These tests verify that:
 * 1. Generated account numbers are always exactly 9 digits
 * 2. Generated account numbers always pass the Luhn check
 * 3. The isValid() method correctly rejects invalid inputs
 * 4. The isValid() method correctly accepts valid inputs
 */
class AccountNumberGeneratorTest {

    // ========================================================================
    // TEST 1: Generated numbers should always be exactly 9 digits
    // ========================================================================
    @Test
    @DisplayName("Generated account number should be exactly 9 digits long")
    void generatedNumberShouldBeNineDigits() {
        // Act — generate a new account number
        String accountNumber = AccountNumberGenerator.generate();

        // Assert — it should be exactly 9 characters
        assertEquals(9, accountNumber.length(),
                "Account number should be exactly 9 digits, but got: " + accountNumber);
    }

    // ========================================================================
    // TEST 2: Generated numbers should always pass their own validation
    // ========================================================================
    @Test
    @DisplayName("Generated account number should pass Luhn validation")
    void generatedNumberShouldPassLuhnCheck() {
        // Act — generate a number
        String accountNumber = AccountNumberGenerator.generate();

        // Assert — the same generator's isValid() should accept it
        assertTrue(AccountNumberGenerator.isValid(accountNumber),
                "Generated account number should be valid, but isValid() returned false for: " + accountNumber);
    }

    // ========================================================================
    // TEST 3: Generate many numbers and ALL should be valid (stress test)
    // ========================================================================
    @Test
    @DisplayName("100 generated account numbers should all be valid")
    void hundredGeneratedNumbersShouldAllBeValid() {
        for (int i = 0; i < 100; i++) {
            String accountNumber = AccountNumberGenerator.generate();

            assertEquals(9, accountNumber.length(),
                    "Iteration " + i + ": length should be 9, got " + accountNumber.length());

            assertTrue(AccountNumberGenerator.isValid(accountNumber),
                    "Iteration " + i + ": generated number " + accountNumber + " failed Luhn check");
        }
    }

    // ========================================================================
    // TEST 4: null should be rejected
    // ========================================================================
    @Test
    @DisplayName("null input should return false")
    void nullShouldBeInvalid() {
        assertFalse(AccountNumberGenerator.isValid(null));
    }

    // ========================================================================
    // TEST 5: Empty string should be rejected
    // ========================================================================
    @Test
    @DisplayName("Empty string should return false")
    void emptyStringShouldBeInvalid() {
        assertFalse(AccountNumberGenerator.isValid(""));
    }

    // ========================================================================
    // TEST 6: Wrong length should be rejected (too short / too long)
    // ========================================================================
    @Test
    @DisplayName("Account number with wrong length should be invalid")
    void wrongLengthShouldBeInvalid() {
        assertFalse(AccountNumberGenerator.isValid("12345678"),
                "8 digits should be invalid");

        assertFalse(AccountNumberGenerator.isValid("1234567890"),
                "10 digits should be invalid");
    }

    // ========================================================================
    // TEST 7: Non-numeric characters should be rejected
    // ========================================================================
    @Test
    @DisplayName("Non-numeric characters should be invalid")
    void nonNumericShouldBeInvalid() {
        assertFalse(AccountNumberGenerator.isValid("12345678A"),
                "Letters should be rejected");

        assertFalse(AccountNumberGenerator.isValid("12345-789"),
                "Special characters should be rejected");
    }

    // ========================================================================
    // TEST 8: A valid number with last digit changed should fail
    // ========================================================================
    @Test
    @DisplayName("Changing last digit of valid number should make it invalid")
    void corruptedLastDigitShouldBeInvalid() {
        // Generate a valid number
        String validNumber = AccountNumberGenerator.generate();

        // Corrupt the last digit (change it to a different value)
        char lastDigit = validNumber.charAt(8);
        char corruptedDigit = (lastDigit == '0') ? '1' : '0';
        String corruptedNumber = validNumber.substring(0, 8) + corruptedDigit;

        // Assert — the corrupted number should fail validation
        assertFalse(AccountNumberGenerator.isValid(corruptedNumber),
                "Corrupted number " + corruptedNumber + " should be invalid (original: " + validNumber + ")");
    }
}
