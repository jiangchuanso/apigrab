package com.gitee.apigrab.core.auth;

import com.jayway.jsonpath.JsonPath;

import java.util.Map;

/**
 * Shared helpers for token-based auth (OAuth2 and PASSWORD_TOKEN): expiry resolution
 * and JSONPath token extraction.
 */
public final class TokenSupport {

    private static final long DEFAULT_EXPIRE_SECONDS = 3600L;
    private static final long EXPIRY_BUFFER_SECONDS = 60L;

    private TokenSupport() {
    }

    /**
     * Effective token lifetime (seconds): configured {@code expireSeconds} &gt; response
     * {@code expires_in} &gt; default 3600.
     */
    public static long resolveExpireSeconds(Integer configured, Long responseExpiresIn) {
        if (configured != null && configured > 0) {
            return configured;
        }
        if (responseExpiresIn != null && responseExpiresIn > 0) {
            return responseExpiresIn;
        }
        return DEFAULT_EXPIRE_SECONDS;
    }

    /** Absolute expiry timestamp with a safety buffer subtracted; never in the past. */
    public static long expireAt(long expiresInSeconds) {
        long ttl = expiresInSeconds > 0 ? expiresInSeconds : DEFAULT_EXPIRE_SECONDS;
        long window = Math.max(ttl - EXPIRY_BUFFER_SECONDS, 1L);
        return System.currentTimeMillis() + window * 1000L;
    }

    /**
     * Extract the token string from a parsed JSON response by JSONPath. When the configured
     * path is blank, {@code defaultPath} is used; if that is also blank the config is invalid.
     */
    public static String extractToken(Map<String, Object> response, String tokenPath, String defaultPath) {
        String path = (tokenPath == null || tokenPath.trim().isEmpty()) ? defaultPath : tokenPath.trim();
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("未配置 Token 提取路径 tokenPath");
        }
        try {
            Object value = JsonPath.read(response, path);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            throw new RuntimeException("按路径 " + path + " 未能从 Token 响应中提取 Token: " + e.getMessage(), e);
        }
    }

    /** Read the optional {@code expires_in} from a token response. */
    public static Long readExpiresIn(Map<String, Object> response) {
        Object value = response == null ? null : response.get("expires_in");
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
