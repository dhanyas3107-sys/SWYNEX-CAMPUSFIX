package com.campusfix;

import com.campusfix.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashIsNotThePlainPassword() {
        String hash = PasswordUtil.hashPassword("Str0ng!Pass");
        assertNotEquals("Str0ng!Pass", hash);
        assertFalse(hash.contains("Str0ng!Pass"));
        assertTrue(hash.startsWith("PBKDF2WithHmacSHA256$"));
    }

    @Test
    void correctPasswordVerifies() {
        String hash = PasswordUtil.hashPassword("Str0ng!Pass");
        assertTrue(PasswordUtil.verifyPassword("Str0ng!Pass", hash));
    }

    @Test
    void wrongPasswordFails() {
        String hash = PasswordUtil.hashPassword("Str0ng!Pass");
        assertFalse(PasswordUtil.verifyPassword("str0ng!pass", hash));
        assertFalse(PasswordUtil.verifyPassword("", hash));
    }

    @Test
    void samePasswordGetsDifferentSaltAndHash() {
        String first = PasswordUtil.hashPassword("Str0ng!Pass");
        String second = PasswordUtil.hashPassword("Str0ng!Pass");
        assertNotEquals(first, second);
        assertTrue(PasswordUtil.verifyPassword("Str0ng!Pass", first));
        assertTrue(PasswordUtil.verifyPassword("Str0ng!Pass", second));
    }

    @Test
    void malformedStoredHashNeverVerifies() {
        assertFalse(PasswordUtil.verifyPassword("anything", "LOGIN_DISABLED"));
        assertFalse(PasswordUtil.verifyPassword("anything", ""));
        assertFalse(PasswordUtil.verifyPassword("anything", null));
        assertFalse(PasswordUtil.verifyPassword("anything", "PBKDF2WithHmacSHA256$abc$@@$@@"));
        assertFalse(PasswordUtil.verifyPassword("anything", "PBKDF2WithHmacSHA256$999999999$AAAA$AAAA"));
        assertFalse(PasswordUtil.verifyPassword(null, "PBKDF2WithHmacSHA256$1$AAAA$AAAA"));
    }

    @Test
    void emptyPasswordCannotBeHashed() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
    }
}
