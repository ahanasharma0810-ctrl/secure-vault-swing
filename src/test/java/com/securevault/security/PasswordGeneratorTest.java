package com.securevault.security;

import com.securevault.exception.ValidationException;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    private static boolean containsAny(String text, String chars) {
        return text.chars().anyMatch(c -> chars.indexOf(c) >= 0);
    }

    @RepeatedTest(20)
    void containsEverySelectedGroup() {
        String pw = generator.generate(new PasswordGenerator.Options().length(8));
        assertEquals(8, pw.length());
        assertTrue(containsAny(pw, PasswordGenerator.UPPERCASE));
        assertTrue(containsAny(pw, PasswordGenerator.LOWERCASE));
        assertTrue(containsAny(pw, PasswordGenerator.DIGITS));
        assertTrue(containsAny(pw, PasswordGenerator.SYMBOLS));
    }

    @Test
    void digitsOnly() {
        String pw = generator.generate(new PasswordGenerator.Options()
                .length(12).uppercase(false).lowercase(false).symbols(false));
        assertTrue(pw.matches("[0-9]{12}"));
    }

    @RepeatedTest(10)
    void canAvoidAmbiguousCharacters() {
        String pw = generator.generate(new PasswordGenerator.Options().length(64).excludeAmbiguous(true));
        assertFalse(containsAny(pw, PasswordGenerator.AMBIGUOUS));
    }

    @Test
    void rejectsNoCharacterTypes() {
        assertThrows(ValidationException.class, () -> generator.generate(new PasswordGenerator.Options()
                .uppercase(false).lowercase(false).digits(false).symbols(false)));
    }

    @Test
    void rejectsInvalidLength() {
        assertThrows(ValidationException.class, () -> generator.generate(new PasswordGenerator.Options().length(2)));
        assertThrows(ValidationException.class, () -> generator.generate(new PasswordGenerator.Options().length(500)));
    }

    @Test
    void passwordsAreDifferent() {
        assertNotEquals(generator.generate(), generator.generate());
    }
}
