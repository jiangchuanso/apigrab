package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.auth.CustomAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomAuthHandlerTest {

    private CustomAuthHandler handler;
    private ApiConfigEntity config;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthHandler();
        config = new ApiConfigEntity();
    }

    @Test
    void testSupportsCustom() {
        assertTrue(handler.supports(AuthType.CUSTOM));
    }

    @Test
    void testDoesNotSupportOther() {
        assertFalse(handler.supports(AuthType.NONE));
        assertFalse(handler.supports(AuthType.BASIC));
    }

    @Test
    void testProcessAuthWithSingleHeader() {
        CustomAuthConfig auth = new CustomAuthConfig();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-API-Key", "abc123");
        auth.setHeaders(headers);
        config.setAuthConfig(auth);
        Headers result = handler.processAuth(config);
        assertEquals("abc123", result.get("X-API-Key"));
        assertEquals(1, result.size());
    }

    @Test
    void testProcessAuthWithMultipleHeaders() {
        CustomAuthConfig auth = new CustomAuthConfig();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-API-Key", "k1");
        headers.put("X-Tenant-Id", "t1");
        auth.setHeaders(headers);
        config.setAuthConfig(auth);
        Headers result = handler.processAuth(config);
        assertEquals("k1", result.get("X-API-Key"));
        assertEquals("t1", result.get("X-Tenant-Id"));
        assertEquals(2, result.size());
    }

    @Test
    void testProcessAuthSkipsBlankKey() {
        CustomAuthConfig auth = new CustomAuthConfig();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("   ", "v");
        headers.put("X-OK", "ok");
        auth.setHeaders(headers);
        config.setAuthConfig(auth);
        Headers result = handler.processAuth(config);
        assertEquals(1, result.size());
        assertEquals("ok", result.get("X-OK"));
    }

    @Test
    void testProcessAuthWithNullConfig() {
        config.setAuthConfig(null);
        assertEquals(0, handler.processAuth(config).size());
    }

    @Test
    void testProcessAuthWithEmptyHeaders() {
        CustomAuthConfig auth = new CustomAuthConfig();
        auth.setHeaders(new LinkedHashMap<>());
        config.setAuthConfig(auth);
        assertEquals(0, handler.processAuth(config).size());
    }
}
