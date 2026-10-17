package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.auth.PasswordTokenAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordTokenAuthHandlerTest {

    private final PasswordTokenAuthHandler handler = new PasswordTokenAuthHandler();

    @Test
    void supportsPasswordToken() {
        assertTrue(handler.supports(AuthType.PASSWORD_TOKEN));
    }

    @Test
    void doesNotSupportOthers() {
        assertFalse(handler.supports(AuthType.OAUTH2));
        assertFalse(handler.supports(AuthType.BASIC));
        assertFalse(handler.supports(AuthType.NONE));
    }

    @Test
    void normalizeLocationDefaultsToHeader() {
        assertEquals("HEADER", PasswordTokenAuthHandler.normalizeLocation(null));
        assertEquals("HEADER", PasswordTokenAuthHandler.normalizeLocation("  "));
        assertEquals("QUERY", PasswordTokenAuthHandler.normalizeLocation("query"));
        assertEquals("BODY", PasswordTokenAuthHandler.normalizeLocation("Body"));
    }

    @Test
    void normalizeLocationRejectsInvalid() {
        assertThrows(CommonException.class, () -> PasswordTokenAuthHandler.normalizeLocation("FOO"));
    }

    @Test
    void contributeRejectsFormatWithoutPlaceholder() {
        ApiConfigEntity config = new ApiConfigEntity();
        PasswordTokenAuthConfig auth = new PasswordTokenAuthConfig();
        auth.setTokenLocation("HEADER");
        auth.setTokenValueFormat("Bearer"); // missing {token}
        config.setAuthConfig(auth);
        CommonException e = assertThrows(CommonException.class, () -> handler.contribute(config));
        assertTrue(e.getMessage().contains("{token}"));
    }

    @Test
    void contributeRejectsUnknownLocation() {
        ApiConfigEntity config = new ApiConfigEntity();
        PasswordTokenAuthConfig auth = new PasswordTokenAuthConfig();
        auth.setTokenLocation("SOMEWHERE");
        config.setAuthConfig(auth);
        assertThrows(CommonException.class, () -> handler.contribute(config));
    }

    @Test
    void contributeRejectsMissingContent() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setAuthConfig(null);
        assertThrows(CommonException.class, () -> handler.contribute(config));
    }

    @Test
    void fetchTokenForTestRejectsMissingContent() {
        assertThrows(CommonException.class, () -> handler.fetchTokenForTest(null));
    }

    @Test
    void buildFormRequestBody() throws Exception {
        PasswordTokenAuthConfig auth = new PasswordTokenAuthConfig();
        auth.setRequestBodyType("FORM");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("user", "admin");
        fields.put("pwd", "secret");
        auth.setBodyFields(fields);
        RequestBody body = PasswordTokenAuthHandler.buildRequestBody(auth);
        assertEquals("user=admin&pwd=secret", bodyToString(body));
    }

    @Test
    void buildJsonRequestBody() throws Exception {
        PasswordTokenAuthConfig auth = new PasswordTokenAuthConfig();
        auth.setRequestBodyType("JSON");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("user", "admin");
        auth.setBodyFields(fields);
        RequestBody body = PasswordTokenAuthHandler.buildRequestBody(auth);
        String json = bodyToString(body);
        assertTrue(json.contains("\"user\":\"admin\""), json);
    }

    private String bodyToString(RequestBody body) throws Exception {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }

    // ---------- 获取 Token 测试（design D2/D3）----------

    @Test
    void fetchTokenForTestWithBlankPathReturnsRawResponseWithoutThrowing() throws Exception {
        injectOkHttp(stubJsonResponse("{\"data\":{\"token\":\"abc\"}}"));
        PasswordTokenAuthConfig auth = new PasswordTokenAuthConfig();
        auth.setTokenUrl("http://localhost/token");
        auth.setRequestBodyType("FORM");
        auth.setBodyFields(new LinkedHashMap<>());
        // tokenPath intentionally blank

        TokenTestResult result = handler.fetchTokenForTest(auth);
        assertNotNull(result.getRawResponse());
        assertTrue(result.getRawResponse().contains("data"), result.getRawResponse());
        assertNull(result.getToken());
        assertNotNull(result.getMessage());
    }

    @Test
    void fetchTokenForTestExtractsTokenByPath() throws Exception {
        injectOkHttp(stubJsonResponse("{\"data\":{\"token\":\"abc\"}}"));
        PasswordTokenAuthConfig auth = new PasswordTokenAuthConfig();
        auth.setTokenUrl("http://localhost/token");
        auth.setRequestBodyType("FORM");
        auth.setBodyFields(new LinkedHashMap<>());
        auth.setTokenPath("$.data.token");

        TokenTestResult result = handler.fetchTokenForTest(auth);
        assertEquals("abc", result.getToken());
        assertNull(result.getMessage());
    }

    @Test
    void fetchTokenForTestNonJsonResponseReportsMessage() throws Exception {
        injectOkHttp(stubJsonResponse("not-json-at-all"));
        PasswordTokenAuthConfig auth = new PasswordTokenAuthConfig();
        auth.setTokenUrl("http://localhost/token");
        auth.setRequestBodyType("FORM");
        auth.setBodyFields(new LinkedHashMap<>());
        auth.setTokenPath("$.data.token");

        TokenTestResult result = handler.fetchTokenForTest(auth);
        assertNotNull(result.getRawResponse());
        assertNull(result.getToken());
        assertNotNull(result.getMessage());
    }

    private void injectOkHttp(OkHttpClient client) throws Exception {
        Field field = PasswordTokenAuthHandler.class.getDeclaredField("okHttpClient");
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
