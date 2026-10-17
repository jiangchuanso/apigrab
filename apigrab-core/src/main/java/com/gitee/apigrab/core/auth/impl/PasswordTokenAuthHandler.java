package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.util.JsonUtils;
import com.gitee.apigrab.core.auth.AuthContribution;
import com.gitee.apigrab.core.auth.AuthHandler;
import com.gitee.apigrab.core.auth.TokenSupport;
import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.auth.PasswordTokenAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PASSWORD_TOKEN auth: POST to {@code tokenUrl} with a user-defined body (JSON or form),
 * extract the token via {@code tokenPath} (JSONPath), then place it at {@code tokenLocation}.
 */
@Slf4j
@Component
public class PasswordTokenAuthHandler implements AuthHandler {

    private static final String LOCATION_HEADER = "HEADER";
    private static final String LOCATION_QUERY = "QUERY";
    private static final String LOCATION_BODY = "BODY";
    private static final String DEFAULT_KEY = "Authorization";
    private static final String DEFAULT_FORMAT = "Bearer {token}";

    private final Map<Long, TokenCache> tokenCache = new ConcurrentHashMap<>();

    @Resource
    private OkHttpClient okHttpClient;

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.PASSWORD_TOKEN;
    }

    @Override
    public Headers processAuth(ApiConfigEntity config) {
        return contribute(config).getHeaders();
    }

    @Override
    public AuthContribution contribute(ApiConfigEntity config) {
        AuthConfig auth = config.getAuthConfig();
        PasswordTokenAuthConfig pwd = auth instanceof PasswordTokenAuthConfig ? (PasswordTokenAuthConfig) auth : null;
        if (pwd == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "账号密码换 Token 认证缺少配置内容");
        }
        String location = normalizeLocation(pwd.getTokenLocation());
        String key = defaultIfBlank(pwd.getTokenKey(), DEFAULT_KEY);
        String format = defaultIfBlank(pwd.getTokenValueFormat(), DEFAULT_FORMAT);
        if (!format.contains("{token}")) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "Token 值格式必须包含 {token} 占位符，当前值: " + format);
        }

        String token = resolveToken(config, pwd);
        String value = format.replace("{token}", token);

        if (LOCATION_QUERY.equals(location)) {
            return new AuthContribution(Headers.of(), Collections.singletonMap(key, value), Collections.emptyMap());
        }
        if (LOCATION_BODY.equals(location)) {
            return new AuthContribution(Headers.of(), Collections.emptyMap(), Collections.singletonMap(key, value));
        }
        return AuthContribution.headersOnly(new Headers.Builder().add(key, value).build());
    }

    /**
     * Fetch the raw token response for the "test" action (no caching, no placement).
     * Used by the auth-config test endpoint. Unlike runtime resolution, a blank {@code tokenPath}
     * or a non-JSON body does not fail the test: the raw response is returned so the UI can render
     * its structure and let the user pick the path (then re-test).
     */
    public TokenTestResult fetchTokenForTest(AuthConfig auth) {
        PasswordTokenAuthConfig pwd = auth instanceof PasswordTokenAuthConfig ? (PasswordTokenAuthConfig) auth : null;
        if (pwd == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "账号密码换 Token 认证配置缺失");
        }
        // Always perform the request first: with a blank path the user still needs the response
        // structure to pick one, so a missing tokenPath must not block the test.
        TokenFetch fetch = requestToken(pwd);
        String tokenPath = pwd.getTokenPath();
        if (tokenPath == null || tokenPath.trim().isEmpty()) {
            return new TokenTestResult(fetch.rawBody, null, "未设置 Token 提取路径，请从下方响应结构中点选");
        }
        if (fetch.parsed == null) {
            return new TokenTestResult(fetch.rawBody, null, "Token 响应不是可解析的 JSON：" + fetch.parseError);
        }
        try {
            String token = TokenSupport.extractToken(fetch.parsed, tokenPath, null);
            return new TokenTestResult(fetch.rawBody, token, null);
        } catch (Exception e) {
            return new TokenTestResult(fetch.rawBody, null, e.getMessage());
        }
    }

    static String normalizeLocation(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return LOCATION_HEADER;
        }
        String value = raw.trim().toUpperCase();
        if (!value.equals(LOCATION_HEADER) && !value.equals(LOCATION_QUERY) && !value.equals(LOCATION_BODY)) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                "无效的 Token 放置位置: " + raw + "（应为 HEADER/QUERY/BODY）");
        }
        return value;
    }

    static String defaultIfBlank(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    private String resolveToken(ApiConfigEntity config, PasswordTokenAuthConfig pwd) {
        Long cacheKey = config.getAuthConfigId();
        boolean cacheable = cacheKey != null;
        if (cacheable) {
            TokenCache cache = tokenCache.get(cacheKey);
            if (cache != null && System.currentTimeMillis() < cache.expireTime) {
                return cache.token;
            }
        }
        TokenResult result = fetchToken(pwd);
        if (cacheable) {
            long expiresIn = TokenSupport.resolveExpireSeconds(pwd.getExpireSeconds(), result.expiresIn);
            tokenCache.put(cacheKey, new TokenCache(result.token, TokenSupport.expireAt(expiresIn)));
        }
        return result.token;
    }

    private TokenResult fetchToken(PasswordTokenAuthConfig pwd) {
        String tokenPath = pwd.getTokenPath();
        if (tokenPath == null || tokenPath.trim().isEmpty()) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "账号密码换 Token 认证缺少 Token 提取路径 tokenPath");
        }
        TokenFetch fetch = requestToken(pwd);
        if (fetch.parsed == null) {
            throw new RuntimeException("Token 响应不是可解析的 JSON: " + fetch.parseError);
        }
        String token = TokenSupport.extractToken(fetch.parsed, tokenPath, null);
        return new TokenResult(token, TokenSupport.readExpiresIn(fetch.parsed));
    }

    /** POST to tokenUrl once, retaining the raw body and an optional JSON parse result. */
    private TokenFetch requestToken(PasswordTokenAuthConfig pwd) {
        String tokenUrl = pwd.getTokenUrl() == null ? "" : pwd.getTokenUrl().trim();
        if (tokenUrl.isEmpty()) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "账号密码换 Token 认证缺少 Token 接口地址 tokenUrl");
        }
        try {
            Request request = new Request.Builder().url(tokenUrl).post(buildRequestBody(pwd)).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Token 请求失败: " + response.code());
                }
                String responseBody = response.body() != null ? response.body().string() : "{}";
                Map<String, Object> parsed = null;
                String parseError = null;
                try {
                    parsed = JsonUtils.parseMap(responseBody);
                } catch (Exception e) {
                    parseError = e.getMessage();
                }
                return new TokenFetch(responseBody, parsed, parseError);
            }
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch PASSWORD_TOKEN token from {}", tokenUrl, e);
            throw new RuntimeException("账号密码换 Token 获取失败: " + e.getMessage(), e);
        }
    }

    /** Build the token request body from the user-defined fields, as JSON or form. */
    static RequestBody buildRequestBody(PasswordTokenAuthConfig pwd) {
        Map<String, String> fields = pwd.getBodyFields() == null ? Collections.emptyMap() : pwd.getBodyFields();
        String format = pwd.getRequestBodyType() == null ? "FORM" : pwd.getRequestBodyType().trim().toUpperCase();
        if ("JSON".equals(format)) {
            Map<String, Object> json = new LinkedHashMap<>();
            fields.forEach((k, v) -> {
                if (k != null && !k.trim().isEmpty()) {
                    json.put(k, v == null ? "" : v);
                }
            });
            return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtils.toJson(json));
        }
        FormBody.Builder form = new FormBody.Builder();
        fields.forEach((k, v) -> {
            if (k != null && !k.trim().isEmpty()) {
                form.add(k, v == null ? "" : v);
            }
        });
        return form.build();
    }

    private static class TokenCache {
        final String token;
        final long expireTime;

        TokenCache(String token, long expireTime) {
            this.token = token;
            this.expireTime = expireTime;
        }
    }

    private static class TokenResult {
        final String token;
        final Long expiresIn;

        TokenResult(String token, Long expiresIn) {
            this.token = token;
            this.expiresIn = expiresIn;
        }
    }

    private static class TokenFetch {
        final String rawBody;
        final Map<String, Object> parsed;
        final String parseError;

        TokenFetch(String rawBody, Map<String, Object> parsed, String parseError) {
            this.rawBody = rawBody;
            this.parsed = parsed;
            this.parseError = parseError;
        }
    }
}
