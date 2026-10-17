package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.auth.BearerAuthConfig;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BearerTokenHandlerTest {

    private BearerTokenHandler handler;
    private ApiConfigEntity config;

    @BeforeEach
    void setUp() {
        handler = new BearerTokenHandler();
        config = new ApiConfigEntity();
    }

    @Test
    void testSupportsBearer() {
        assertTrue(handler.supports(AuthType.BEARER));
    }

    @Test
    void testDoesNotSupportOther() {
        assertFalse(handler.supports(AuthType.NONE));
        assertFalse(handler.supports(AuthType.BASIC));
    }

    @Test
    void testProcessAuth() {
        BearerAuthConfig auth = new BearerAuthConfig();
        auth.setToken("my-jwt-token-123");
        config.setAuthConfig(auth);
        Headers headers = handler.processAuth(config);
        assertEquals("Bearer my-jwt-token-123", headers.get("Authorization"));
    }

    @Test
    void testProcessAuthWithEmptyToken() {
        BearerAuthConfig auth = new BearerAuthConfig();
        auth.setToken("");
        config.setAuthConfig(auth);
        Headers headers = handler.processAuth(config);
        assertEquals("Bearer", headers.get("Authorization"));
    }

    @Test
    void testProcessAuthWithNullConfig() {
        config.setAuthConfig(null);
        Headers headers = handler.processAuth(config);
        assertEquals("Bearer", headers.get("Authorization"));
    }
}
