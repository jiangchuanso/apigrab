package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.auth.OAuth2AuthConfig;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2HandlerTest {

    private OAuth2Handler handler;
    private ApiConfigEntity config;

    @BeforeEach
    void setUp() {
        handler = new OAuth2Handler();
        config = new ApiConfigEntity();
        config.setId(1L);
    }

    @Test
    void testSupportsOAuth2() {
        assertTrue(handler.supports(AuthType.OAUTH2));
    }

    @Test
    void testDoesNotSupportOther() {
        assertFalse(handler.supports(AuthType.NONE));
        assertFalse(handler.supports(AuthType.BASIC));
        assertFalse(handler.supports(AuthType.BEARER));
    }

    @Test
    void testParseAuthConfig() {
        OAuth2AuthConfig auth = new OAuth2AuthConfig();
        auth.setTokenUrl("https://example.com/token");
        auth.setClientId("app1");
        auth.setClientSecret("secret");
        config.setAuthConfig(auth);
        // No real token server: acquisition fails
        assertThrows(RuntimeException.class, () -> handler.processAuth(config));
    }

    @Test
    void testParseAuthConfigWithEmptyTokenUrl() {
        OAuth2AuthConfig auth = new OAuth2AuthConfig();
        auth.setTokenUrl("");
        auth.setClientId("");
        auth.setClientSecret("");
        config.setAuthConfig(auth);
        assertThrows(RuntimeException.class, () -> handler.processAuth(config));
    }

    @Test
    void testParseAuthConfigNull() {
        config.setAuthConfig(null);
        assertThrows(RuntimeException.class, () -> handler.processAuth(config));
    }

    @Test
    void fetchTokenForTestRejectsMissingConfig() {
        assertThrows(CommonException.class, () -> handler.fetchTokenForTest(null));
    }

    // ---------- 获取 Token 测试（design D2）----------

    @Test
    void fetchTokenForTestUsesDefaultPathAndReturnsRawResponse() throws Exception {
        injectOkHttp(stubJsonResponse("{\"access_token\":\"tok\",\"expires_in\":7200}"));
        OAuth2AuthConfig auth = new OAuth2AuthConfig();
        auth.setTokenUrl("http://localhost/token");
        auth.setClientId("id");
        auth.setClientSecret("secret");
        // tokenPath intentionally blank -> default $.access_token

        TokenTestResult result = handler.fetchTokenForTest(auth);
        assertEquals("tok", result.getToken());
        assertNotNull(result.getRawResponse());
        assertTrue(result.getRawResponse().contains("access_token"), result.getRawResponse());
        assertNull(result.getMessage());
    }

    @Test
    void fetchTokenForTestExtractsByCustomPath() throws Exception {
        injectOkHttp(stubJsonResponse("{\"data\":{\"token\":\"custom\"}}"));
        OAuth2AuthConfig auth = new OAuth2AuthConfig();
        auth.setTokenUrl("http://localhost/token");
        auth.setClientId("id");
        auth.setClientSecret("secret");
        auth.setTokenPath("$.data.token");

        TokenTestResult result = handler.fetchTokenForTest(auth);
        assertEquals("custom", result.getToken());
    }

    @Test
    void fetchTokenForTestNonJsonReportsMessage() throws Exception {
        injectOkHttp(stubJsonResponse("not-json"));
        OAuth2AuthConfig auth = new OAuth2AuthConfig();
        auth.setTokenUrl("http://localhost/token");
        auth.setClientId("id");
        auth.setClientSecret("secret");

        TokenTestResult result = handler.fetchTokenForTest(auth);
        assertNull(result.getToken());
        assertNotNull(result.getMessage());
        assertNotNull(result.getRawResponse());
    }

    private void injectOkHttp(OkHttpClient client) throws Exception {
        Field field = OAuth2Handler.class.getDeclaredField("okHttpClient");
        field.setAccessible(true);
        field.set(handler, client);
    }

    private static OkHttpClient stubJsonResponse(String body) throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        Response response = new Response.Builder()
            .request(new Request.Builder().url("http://localhost/token").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(MediaType.parse("application/json"), body))
            .build();
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        return client;
    }
}
