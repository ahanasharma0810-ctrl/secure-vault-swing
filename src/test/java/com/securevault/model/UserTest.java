package com.securevault.model;

import com.securevault.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void createsUser() {
        User user = new User("demo_user", "hash", "salt1", "salt2");
        assertEquals("demo_user", user.getUsername());
        assertEquals("demo_user", user.getDisplayName());
    }

    @Test
    void rejectsInvalidUsernames() {
        assertThrows(ValidationException.class, () -> new User("", "h", "s", "k"));
        assertThrows(ValidationException.class, () -> new User("has space", "h", "s", "k"));
        assertThrows(ValidationException.class, () -> new User("a".repeat(33), "h", "s", "k"));
    }

    @Test
    void requiresHashAndSalts() {
        assertThrows(ValidationException.class, () -> new User("demo_user", "", "s", "k"));
        assertThrows(ValidationException.class, () -> new User("demo_user", "h", null, "k"));
    }

    @Test
    void toStringHidesSecrets() {
        String text = new User("demo_user", "SECRET_HASH", "SALT_A", "SALT_B").toString();
        assertFalse(text.contains("SECRET_HASH"));
        assertFalse(text.contains("SALT_A"));
    }
}
