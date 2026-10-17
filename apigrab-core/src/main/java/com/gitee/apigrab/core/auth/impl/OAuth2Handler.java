package com.gitee.apigrab.core.auth.impl;

import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.util.JsonUtils;
import com.gitee.apigrab.core.auth.AuthHandler;
import com.gitee.apigrab.core.auth.TokenSupport;
import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.auth.OAuth2AuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OAuth2Handler implements AuthHandler {

    private static final String DEFAULT_TOKEN_PATH = "$.access_token";

    /** Token cache keyed by the shared auth config id (many api configs may share one auth). */
    private final Map<Long, TokenCache> tokenCache = new ConcurrentHashMap<>();

    @Resource
    private OkHttpClient okHttpClient;

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.OAUTH2;
    }

    @Override
    public Headers processAuth(ApiConfigEntity config) {
        AuthConfig auth = config.getAuthConfig();
        OAuth2AuthConfig oauth = auth instanceof OAuth2AuthConfig ? (OAuth2AuthConfig) auth : null;
        String tokenUrl = oauth == null || oauth.getTokenUrl() == null ? "" : oauth.getTokenUrl();
        String clientId = oauth == null || oauth.getClientId() == null ? "" : oauth.getClientId();
        String clientSecret = oauth == null || oauth.getClientSecret() == null ? "" : oauth.getClientSecret();
        String scope = oauth == null || oauth.getScope() == null ? "" : oauth.getScope();
        String tokenPath = oauth == null ? null : oauth.getTokenPath();

        Long cacheKey = config.getAuthConfigId();
        boolean cacheable = cacheKey != null;
        if (cacheable) {
            TokenCache cache = tokenCache.get(cacheKey);
            if (cache != null && System.currentTimeMillis() < cache.expireTime) {
                return new Headers.Builder().add("Authorization", "Bearer " + cache.token).build();
            }
        }

        TokenResult result = fetchToken(tokenUrl, clientId, clientSecret, scope, tokenPath);

        if (cacheable) {
            Integer configured = oauth == null ? null : oauth.getExpireSeconds();
            long expiresIn = TokenSupport.resolveExpireSeconds(configured, result.expiresIn);
            tokenCache.put(cacheKey, new TokenCache(result.token, TokenSupport.expireAt(expiresIn)));
        }

        return new Headers.Builder().add("Authorization", "Bearer " + result.token).build();
    }

    /**
     * Fetch the raw token response for the "test" action (no caching, no header build).
     * Used by the auth-config test endpoint. Returns the raw body so the UI can render its
     * structure; a non-JSON body or an extraction failure is reported via {@code message}
     * instead of throwing, so "test first, then pick a path" is possible.
     */
    public TokenTestResult fetchTokenForTest(AuthConfig auth) {
        OAuth2AuthConfig oauth = auth instanceof OAuth2AuthConfig ? (OAuth2AuthConfig) auth : null;
        if (oauth == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "OAuth2 认证配置缺失");
        }
        String tokenUrl = oauth.getTokenUrl() == null ? "" : oauth.getTokenUrl();
        String clientId = oauth.getClientId() == null ? "" : oauth.getClientId();
        String clientSecret = oauth.getClientSecret() == null ? "" : oauth.getClientSecret();
        String scope = oauth.getScope() == null ? "" : oauth.getScope();

        TokenFetch fetch = fetchRaw(tokenUrl, clientId, clientSecret, scope);
        if (fetch.parsed == null) {
            return new TokenTestResult(fetch.rawBody, null, "Token 响应不是可解析的 JSON：" + fetch.parseError);
        }
        try {
            String token = TokenSupport.extractToken(fetch.parsed, oauth.getTokenPath(), DEFAULT_TOKEN_PATH);
            return new TokenTestResult(fetch.rawBody, token, null);
        } catch (Exception e) {
            return new TokenTestResult(fetch.rawBody, null, e.getMessage());
        }
    }

    private TokenResult fetchToken(String tokenUrl, String clientId, String clientSecret, String scope, String tokenPath) {
        TokenFetch fetch = fetchRaw(tokenUrl, clientId, clientSecret, scope);
        if (fetch.parsed == null) {
            throw new RuntimeException("OAuth2 token response is not JSON: " + fetch.parseError);
        }
        String token = TokenSupport.extractToken(fetch.parsed, tokenPath, DEFAULT_TOKEN_PATH);
        return new TokenResult(token, TokenSupport.readExpiresIn(fetch.parsed));
    }

    /**
     * Perform the token request once and retain the raw body plus an optional parse result.
     * Transport failures (connection error or non-2xx) still throw; a body that is not JSON
     * is captured in {@code parseError} rather than thrown so callers can decide.
     */
    private TokenFetch fetchRaw(String tokenUrl, String clientId, String clientSecret, String scope) {
        try {
            FormBody.Builder formBuilder = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("client_secret", clientSecret);
            if (scope != null && !scope.isEmpty()) {
                formBuilder.add("scope", scope);
            }

            Request request = new Request.Builder()
                .url(tokenUrl)
                .post(formBuilder.build())
                .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OAuth2 token request failed: " + response.code());
                }
                String body = response.body() != null ? response.body().string() : "{}";
                Map<String, Object> parsed = null;
                String parseError = null;
                try {
                    parsed = JsonUtils.parseMap(body);
                } catch (Exception e) {
                    parseError = e.getMessage();
                }
                return new TokenFetch(body, parsed, parseError);
            }
        } catch (Exception e) {
            log.error("Failed to fetch OAuth2 token from {}", tokenUrl, e);
            throw new RuntimeException("OAuth2 token acquisition failed: " + e.getMessage(), e);
        }
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
