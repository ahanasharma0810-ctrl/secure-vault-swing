package com.securevault.security;

import com.securevault.model.PasswordStrength;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordStrengthCheckerTest {

    private final PasswordStrengthChecker checker = new PasswordStrengthChecker();

    @Test
    void emptyAndCommonPasswordsAreVeryWeak() {
        assertEquals(PasswordStrength.VERY_WEAK, checker.evaluate(""));
        assertEquals(PasswordStrength.VERY_WEAK, checker.evaluate(null));
        assertEquals(PasswordStrength.VERY_WEAK, checker.evaluate("password"));
        assertEquals(PasswordStrength.VERY_WEAK, checker.evaluate("abc"));
    }

    @Test
    void longMixedPasswordIsVeryStrong() {
        assertEquals(PasswordStrength.VERY_STRONG, checker.evaluate("Tr0ub4dor&3-Horse!"));
    }

    @Test
    void strengthGrowsWithVariety() {
        int weak = checker.evaluate("hello123").getScore();
        int better = checker.evaluate("Hello123!x").getScore();
        int best = checker.evaluate("Hello123!xYz#9876").getScore();
        assertTrue(weak < better);
        assertTrue(better < best);
    }
}
