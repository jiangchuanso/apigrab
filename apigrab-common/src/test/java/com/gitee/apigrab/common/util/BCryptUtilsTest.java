package com.gitee.apigrab.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptUtilsTest {

    @Test
    void hashesAndMatches() {
        String hash = BCryptUtils.hashPassword("admin123");
        assertNotNull(hash);
        assertNotEquals("admin123", hash);
        assertTrue(BCryptUtils.matches("admin123", hash));
    }

    @Test
    void rejectsWrongPassword() {
        String hash = BCryptUtils.hashPassword("admin123");
        assertFalse(BCryptUtils.matches("wrong", hash));
    }

    @Test
    void handlesBlankInputs() {
        assertFalse(BCryptUtils.matches(null, "$2a$10$abcdefghijklmnopqrstuv"));
        assertFalse(BCryptUtils.matches("x", null));
        assertThrows(IllegalArgumentException.class, () -> BCryptUtils.hashPassword(""));
    }
}
