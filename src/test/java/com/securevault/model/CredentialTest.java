package com.securevault.model;

import com.securevault.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialTest {

    private Credential sample() {
        return new Credential("gmail.com", "student.demo@gmail.com", "Secret#123", "Email", "personal");
    }

    @Test
    void createsCredentialWithAllFields() {
        Credential c = sample();
        assertEquals("gmail.com", c.getWebsite());
        assertEquals("student.demo@gmail.com", c.getUsername());
        assertEquals("Secret#123", c.getPassword());
        assertEquals("Email", c.getCategory());
        assertNotNull(c.getId());
        assertNotNull(c.getCreatedAt());
    }

    @Test
    void trimsTextFields() {
        Credential c = new Credential("  github.com ", " demo_user ", "pw", " Work ", " note ");
        assertEquals("github.com", c.getWebsite());
        assertEquals("demo_user", c.getUsername());
        assertEquals("Work", c.getCategory());
        assertEquals("note", c.getNotes());
    }

    @Test
    void blankCategoryBecomesGeneral() {
        Credential c = new Credential("site", "user", "pw", "  ", null);
        assertEquals(Credential.DEFAULT_CATEGORY, c.getCategory());
        assertEquals("", c.getNotes());
    }

    @Test
    void rejectsEmptyRequiredFields() {
        assertThrows(ValidationException.class, () -> new Credential("", "user", "pw", null, null));
        assertThrows(ValidationException.class, () -> new Credential("site", " ", "pw", null, null));
        assertThrows(ValidationException.class, () -> new Credential("site", "user", "", null, null));
    }

    @Test
    void rejectsTooLongValues() {
        String longText = new String(new char[201]).replace('\0', 'a');
        assertThrows(ValidationException.class, () -> new Credential(longText, "user", "pw", null, null));
    }

    @Test
    void searchMatchesAnyTextField() {
        Credential c = sample();
        assertTrue(c.matches("GMAIL"));
        assertTrue(c.matches("email"));
        assertTrue(c.matches("personal"));
        assertTrue(c.matches(""));
        assertFalse(c.matches("facebook"));
    }

    @Test
    void equalityIsBasedOnId() {
        Credential a = sample();
        Credential sameId = new Credential(a.getId(), a.getCreatedAt(), "other.com", "x", "y", null, null);
        assertEquals(a, sameId);
        assertEquals(a.hashCode(), sameId.hashCode());
        assertNotEquals(a, sample());
    }

    @Test
    void toStringHidesPassword() {
        assertFalse(sample().toString().contains("Secret#123"));
    }
}
