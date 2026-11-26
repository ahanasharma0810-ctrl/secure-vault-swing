package com.securevault.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void blankChecks() {
        assertTrue(InputValidator.isBlank(null));
        assertTrue(InputValidator.isBlank("   "));
        assertFalse(InputValidator.isBlank(" a "));
    }

    @Test
    void emailCheck() {
        assertTrue(InputValidator.looksLikeEmail("student.demo@gmail.com"));
        assertFalse(InputValidator.looksLikeEmail("demo_user"));
        assertFalse(InputValidator.looksLikeEmail("a@b"));
    }

    @Test
    void normalisesWebsites() {
        assertEquals("gmail.com", InputValidator.normalizeWebsite("https://www.Gmail.com/inbox"));
        assertEquals("github.com", InputValidator.normalizeWebsite("  http://github.com "));
        assertEquals("Bank App", InputValidator.normalizeWebsite(" Bank App "));
        assertEquals("Netflix", InputValidator.normalizeWebsite("Netflix"));
        assertNull(InputValidator.normalizeWebsite(null));
    }

    @Test
    void buildsWebAddresses() {
        assertEquals("https://gmail.com", InputValidator.toWebAddress("gmail.com"));
        assertEquals("http://example.org", InputValidator.toWebAddress("http://example.org"));
        assertNull(InputValidator.toWebAddress("Bank App"));
        assertNull(InputValidator.toWebAddress("Netflix"));
        assertNull(InputValidator.toWebAddress(" "));
    }

    @Test
    void removesControlCharacters() {
        assertEquals("demo_user", InputValidator.singleLine("\tdemo_user\n"));
    }
}
