package com.gitee.apigrab.core.http;

import com.gitee.apigrab.core.auth.AuthContribution;
import com.gitee.apigrab.core.auth.AuthHandlerChain;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.util.JsonUtils;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 请求体构造契约。
 *
 * <p>重点回归 XML：此前 XML 落到兜底分支，会被发成**空正文 + application/json**。
 */
class HttpRequestBuilderTest {

    private HttpRequestBuilder builder;
    private AuthHandlerChain authHandlerChain;

    @BeforeEach
    void setUp() throws Exception {
        builder = new HttpRequestBuilder();
        authHandlerChain = mock(AuthHandlerChain.class);
        when(authHandlerChain.handle(any(ApiConfigEntity.class)))
            .thenReturn(AuthContribution.empty());
        inject(builder, "authHandlerChain", authHandlerChain);
    }

    @Test
    void xmlBodyIsSentAsApplicationXmlWithContentIntact() throws IOException {
        ApiConfigEntity config = config(BodyType.XML, "<request><page>1</page></request>");

        Request request = builder.build(config);

        assertEquals("POST", request.method());
        assertNotNull(request.body());
        MediaType contentType = request.body().contentType();
        assertNotNull(contentType, "XML 正文必须带 Content-Type");
        assertEquals("application", contentType.type());
        assertEquals("xml", contentType.subtype());
        // 断言实际发出的 Content-Type 字面量（charset 参数大小写由 OkHttp 规范化，故忽略大小写）
        assertTrue(contentType.toString().equalsIgnoreCase("application/xml; charset=utf-8"),
            "实际: " + contentType);
        assertEquals("<request><page>1</page></request>", bodyOf(request));
    }

    @Test
    void xmlBodyWithNullContentSendsEmptyBodyNotJson() throws IOException {
        Request request = builder.build(config(BodyType.XML, null));

        assertEquals("xml", request.body().contentType().subtype(),
            "XML 未填正文时仍不得退回 application/json");
        assertEquals("", bodyOf(request));
    }

    @Test
    void jsonBodyBehaviourUnchanged() throws IOException {
        Request request = builder.build(config(BodyType.JSON, "{\"a\":1}"));

        assertEquals("application", request.body().contentType().type());
        assertEquals("json", request.body().contentType().subtype());
        assertEquals("{\"a\":1}", bodyOf(request));
    }

    @Test
    void formBodySendsEncodedPairsAsFormFields() throws IOException {
        // FORM 正文契约：application/x-www-form-urlencoded 编码串（k1=v1&k2=v2），
        // 不再是 JSON 对象。见 createFormBody。
        Request request = builder.build(config(BodyType.FORM, "a=1&b=2"));

        assertEquals("a=1&b=2", bodyOf(request));
        Map<String, String> fields = parseForm(bodyOf(request));
        assertEquals("1", fields.get("a"));
        assertEquals("2", fields.get("b"));
    }

    @Test
    void formBodyKeepsAmpersandInsideValueAndDoesNotDoubleEncode() throws IOException {
        // 值里的 & 以 %26 存储；目标端应解码回一个字段值 a&b，且不得出现双重编码 %2526
        Request request = builder.build(config(BodyType.FORM, "q=a%26b"));
        String raw = bodyOf(request);

        Map<String, String> fields = parseForm(raw);
        assertEquals("a&b", fields.get("q"), "实际: " + raw);
        assertEquals(1, fields.size(), "& 不得被当成字段分隔符");
        assertFalse(raw.contains("%2526"), "不得双重编码：实际 " + raw);
    }

    @Test
    void formBodyRoundTripsReservedCharactersAndUnicode() throws IOException {
        Request request = builder.build(config(BodyType.FORM,
            "eq=x%3Dy&plus=x%2By&space=x+y&pct=100%25&cjk=%E4%B8%AD%E6%96%87"));

        Map<String, String> fields = parseForm(bodyOf(request));
        assertEquals("x=y", fields.get("eq"));
        assertEquals("x+y", fields.get("plus"));
        assertEquals("x y", fields.get("space"));
        assertEquals("100%", fields.get("pct"));
        assertEquals("中文", fields.get("cjk"));
    }

    @Test
    void formBodyEmptyWhenContentNullOrBlank() throws IOException {
        assertEquals("", bodyOf(builder.build(config(BodyType.FORM, null))));
        assertEquals("", bodyOf(builder.build(config(BodyType.FORM, ""))));
    }

    @Test
    void authHeaderIsApplied() {
        ApiConfigEntity config = config(BodyType.JSON, "{\"a\":1}");
        when(authHandlerChain.handle(config)).thenReturn(new AuthContribution(
            new Headers.Builder().add("Authorization", "Bearer tok").build(), null, null));
        Request request = builder.build(config);
        assertEquals("Bearer tok", request.header("Authorization"));
    }

    @Test
    void authQueryParamIsAppended() {
        ApiConfigEntity config = config(BodyType.NONE, null);
        config.setMethod(HttpMethod.GET);
        when(authHandlerChain.handle(config)).thenReturn(new AuthContribution(
            null, Collections.singletonMap("access_token", "tok"), null));
        Request request = builder.build(config);
        assertTrue(request.url().toString().contains("access_token=tok"), "url=" + request.url());
    }

    @Test
    void authBodyFieldInjectedIntoJson() throws IOException {
        ApiConfigEntity config = config(BodyType.JSON, "{\"a\":1}");
        when(authHandlerChain.handle(config)).thenReturn(new AuthContribution(
            null, null, Collections.singletonMap("token", "tok")));
        Request request = builder.build(config);
        Map<String, Object> body = JsonUtils.parseMap(bodyOf(request));
        assertEquals("1", String.valueOf(body.get("a")));
        assertEquals("tok", String.valueOf(body.get("token")));
    }

    @Test
    void authBodyFieldInjectedIntoForm() throws IOException {
        ApiConfigEntity config = config(BodyType.FORM, "a=1");
        when(authHandlerChain.handle(config)).thenReturn(new AuthContribution(
            null, null, Collections.singletonMap("token", "tok")));
        Request request = builder.build(config);
        Map<String, String> fields = parseForm(bodyOf(request));
        assertEquals("1", fields.get("a"));
        assertEquals("tok", fields.get("token"));
    }

    @Test
    void authBodyFieldRejectedForXml() {
        ApiConfigEntity config = config(BodyType.XML, "<x/>");
        when(authHandlerChain.handle(config)).thenReturn(new AuthContribution(
            null, null, Collections.singletonMap("token", "tok")));
        assertThrows(CommonException.class, () -> builder.build(config));
    }

    @Test
    void authBodyFieldRejectedForGet() {
        ApiConfigEntity config = config(BodyType.NONE, null);
        config.setMethod(HttpMethod.GET);
        when(authHandlerChain.handle(config)).thenReturn(new AuthContribution(
            null, null, Collections.singletonMap("token", "tok")));
        assertThrows(CommonException.class, () -> builder.build(config));
    }

    private String bodyOf(Request request) throws IOException {
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }

    /** 按表单规则解析实际发出的正文，用于断言目标端收到的字段语义。 */
    private Map<String, String> parseForm(String body) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        if (body == null || body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            map.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
        }
        return map;
    }

    private ApiConfigEntity config(BodyType bodyType, String bodyContent) {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(bodyType);
        config.setBodyContent(bodyContent);
        return config;
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
