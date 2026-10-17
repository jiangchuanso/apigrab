package com.gitee.apigrab.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilsTest {

    @Test
    void testEncryptAndDecrypt() {
        String original = "myDatabasePassword!@#";
        String encrypted = PasswordUtils.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = PasswordUtils.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptNull() {
        assertNull(PasswordUtils.encrypt(null));
    }

    @Test
    void testDecryptNull() {
        assertNull(PasswordUtils.decrypt(null));
    }

    @Test
    void testEmptyString() {
        String encrypted = PasswordUtils.encrypt("");
        assertNotNull(encrypted);
        String decrypted = PasswordUtils.decrypt(encrypted);
        assertEquals("", decrypted);
    }

    @Test
    void testSpecialCharacters() {
        String original = "Aa1!@#$%^&*()_+-=[]{}|;':\",./<>?`~你好";
        String encrypted = PasswordUtils.encrypt(original);
        String decrypted = PasswordUtils.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testDeterministic() {
        String pwd = "test123";
        String e1 = PasswordUtils.encrypt(pwd);
        String decrypted = PasswordUtils.decrypt(e1);
        assertEquals(pwd, decrypted);
    }
}
