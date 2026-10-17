package com.gitee.apigrab.core.auth;

import com.gitee.apigrab.core.auth.impl.BasicAuthHandler;
import com.gitee.apigrab.core.auth.impl.BearerTokenHandler;
import com.gitee.apigrab.core.auth.impl.CustomAuthHandler;
import com.gitee.apigrab.core.auth.impl.NoAuthHandler;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.core.auth.impl.*;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.auth.BasicAuthConfig;
import com.gitee.apigrab.persistence.entity.auth.BearerAuthConfig;
import com.gitee.apigrab.persistence.entity.auth.CustomAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthHandlerChainTest {

    private AuthHandlerChain chain;

    @BeforeEach
    void setUp() {
        chain = new AuthHandlerChain(Arrays.asList(
            new NoAuthHandler(),
            new BasicAuthHandler(),
            new BearerTokenHandler(),
            new CustomAuthHandler()
        ));
    }

    private Headers headersOf(ApiConfigEntity config) {
        return chain.handle(config).getHeaders();
    }

    @Test
    void testHandleNone() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthType(AuthType.NONE);
        assertEquals(0, headersOf(config).size());
    }

    @Test
    void testHandleNullType() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthType(null);
        assertEquals(0, headersOf(config).size());
    }

    @Test
    void testHandleBasic() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthType(AuthType.BASIC);
        BasicAuthConfig auth = new BasicAuthConfig();
        auth.setUsername("admin");
        auth.setPassword("pass");
        config.setAuthConfig(auth);
        Headers headers = headersOf(config);
        assertNotNull(headers.get("Authorization"));
        assertTrue(headers.get("Authorization").startsWith("Basic "));
    }

    @Test
    void testHandleBearer() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthType(AuthType.BEARER);
        BearerAuthConfig auth = new BearerAuthConfig();
        auth.setToken("test-token");
        config.setAuthConfig(auth);
        assertEquals("Bearer test-token", headersOf(config).get("Authorization"));
    }

    @Test
    void testHandleCustom() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthType(AuthType.CUSTOM);
        CustomAuthConfig auth = new CustomAuthConfig();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Key", "val");
        auth.setHeaders(headers);
        config.setAuthConfig(auth);
        assertEquals("val", headersOf(config).get("X-Key"));
    }

    @Test
    void testHandleUnsupportedType() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthType(AuthType.OAUTH2); // not registered in this chain
        CommonException e = assertThrows(CommonException.class, () -> chain.handle(config));
        assertTrue(e.getMessage().contains("OAUTH2"));
    }

    @Test
    void testEmptyHandlerList() {
        AuthHandlerChain emptyChain = new AuthHandlerChain(Collections.emptyList());
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthType(AuthType.NONE);
        assertThrows(CommonException.class, () -> emptyChain.handle(config));
    }
}
