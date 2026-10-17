package com.gitee.apigrab.core.auth;

import okhttp3.Headers;

import java.util.Collections;
import java.util.Map;

/**
 * Result of applying an auth handler: it can contribute request headers, URL query
 * parameters, and request-body fields. Existing auth types only contribute headers;
 * the PASSWORD_TOKEN type may place its token into query or body as well.
 */
public class AuthContribution {

    private final Headers headers;
    private final Map<String, String> queryParams;
    private final Map<String, String> bodyFields;

    public AuthContribution(Headers headers, Map<String, String> queryParams, Map<String, String> bodyFields) {
        this.headers = headers != null ? headers : new Headers.Builder().build();
        this.queryParams = queryParams != null ? queryParams : Collections.emptyMap();
        this.bodyFields = bodyFields != null ? bodyFields : Collections.emptyMap();
    }

    public static AuthContribution headersOnly(Headers headers) {
        return new AuthContribution(headers, Collections.emptyMap(), Collections.emptyMap());
    }

    public static AuthContribution empty() {
        return headersOnly(new Headers.Builder().build());
    }

    public Headers getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getBodyFields() {
        return bodyFields;
    }
}
