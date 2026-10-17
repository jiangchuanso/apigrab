package com.gitee.apigrab.admin.security;

import javax.servlet.http.HttpServletRequest;

/**
 * Extracts the access token from {@code Authorization: Bearer <token>} or the
 * {@code token} request parameter.
 */
public final class TokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private TokenResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null) {
            String value = header.trim();
            if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                return value.substring(BEARER_PREFIX.length()).trim();
            }
            if (!value.isEmpty()) {
                return value;
            }
        }
        return request.getParameter("token");
    }
}
