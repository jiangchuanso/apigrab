package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.auth.BasicAuthConfig;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicAuthHandlerTest {

    private BasicAuthHandler handler;
    private ApiConfigEntity config;

    @BeforeEach
    void setUp() {
        handler = new BasicAuthHandler();
        config = new ApiConfigEntity();
    }

    @Test
    void testSupportsBasic() {
        assertTrue(handler.supports(AuthType.BASIC));
    }

    @Test
    void testDoesNotSupportOther() {
        assertFalse(handler.supports(AuthType.NONE));
        assertFalse(handler.supports(AuthType.BEARER));
    }

    @Test
    void testProcessAuth() {
        BasicAuthConfig auth = new BasicAuthConfig();
        auth.setUsername("admin");
        auth.setPassword("secret123");
        config.setAuthConfig(auth);
        Headers headers = handler.processAuth(config);
        String authHeader = headers.get("Authorization");
        assertNotNull(authHeader);
        String expectedEncoded = java.util.Base64.getEncoder().encodeToString("admin:secret123".getBytes());
        assertEquals("Basic " + expectedEncoded, authHeader);
    }

    @Test
    void testProcessAuthWithNullConfig() {
        config.setAuthConfig(null);
        Headers headers = handler.processAuth(config);
        assertNotNull(headers.get("Authorization"));
        assertTrue(headers.get("Authorization").startsWith("Basic "));
    }

    @Test
    void testProcessAuthWithSpecialChars() {
        BasicAuthConfig auth = new BasicAuthConfig();
        auth.setUsername("user@corp");
        auth.setPassword("p@ss!");
        config.setAuthConfig(auth);
        Headers headers = handler.processAuth(config);
        String expectedEncoded = java.util.Base64.getEncoder().encodeToString("user@corp:p@ss!".getBytes());
        assertEquals("Basic " + expectedEncoded, headers.get("Authorization"));
    }
}
