package com.securevault.security;

import com.securevault.model.PasswordStrength;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Rates a password from VERY_WEAK to VERY_STRONG using
 * length, character variety and a list of very common passwords.
 */
public class PasswordStrengthChecker {

    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "password1", "password123", "123456", "12345678", "123456789",
            "qwerty", "qwerty123", "abc123", "111111", "letmein", "welcome", "admin",
            "iloveyou", "monkey", "dragon", "football", "baseball", "sunshine", "princess"));

    public PasswordStrength evaluate(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.VERY_WEAK;
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            return PasswordStrength.VERY_WEAK;
        }

        int score = 0;

        // Length
        int length = password.length();
        if (length >= 8) score++;
        if (length >= 12) score++;
        if (length >= 16) score++;

        // Variety
        int types = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) types++;
        if (password.chars().anyMatch(Character::isLowerCase)) types++;
        if (password.chars().anyMatch(Character::isDigit)) types++;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) types++;
        if (types >= 2) score++;
        if (types >= 3) score++;
        if (types == 4) score++;

        // Penalties
        if (password.matches(".*(.)\\1\\1.*")) score--;          // aaa, 111
        if (password.matches("[0-9]+") || password.matches("[a-zA-Z]+")) score--;
        if (length < 6) score = Math.min(score, 0);

        if (score <= 1) return PasswordStrength.VERY_WEAK;
        if (score == 2) return PasswordStrength.WEAK;
        if (score == 3 || score == 4) return PasswordStrength.MEDIUM;
        if (score == 5) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }
}
