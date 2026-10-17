package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoAuthHandlerTest {

    private NoAuthHandler handler;
    private ApiConfigEntity config;

    @BeforeEach
    void setUp() {
        handler = new NoAuthHandler();
        config = new ApiConfigEntity();
    }

    @Test
    void testSupportsNone() {
        assertTrue(handler.supports(AuthType.NONE));
    }

    @Test
    void testSupportsNull() {
        assertTrue(handler.supports(null));
    }

    @Test
    void testDoesNotSupportOtherTypes() {
        assertFalse(handler.supports(AuthType.BASIC));
        assertFalse(handler.supports(AuthType.BEARER));
        assertFalse(handler.supports(AuthType.OAUTH2));
        assertFalse(handler.supports(AuthType.CUSTOM));
    }

    @Test
    void testProcessAuthReturnsEmptyHeaders() {
        config.setAuthType(AuthType.NONE);
        Headers headers = handler.processAuth(config);
        assertEquals(0, headers.size());
    }
}
