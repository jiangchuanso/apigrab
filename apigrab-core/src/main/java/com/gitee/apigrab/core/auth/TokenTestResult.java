package com.gitee.apigrab.core.auth;

/**
 * Outcome of a "test get token" request.
 *
 * <p>Carries the raw token response body so the UI can render its structure and let the user
 * pick the extraction path, plus the optionally extracted token and a readable diagnostic
 * message when extraction was not possible (e.g. blank path or a non-JSON response).
 */
public class TokenTestResult {

    private final String rawResponse;
    private final String token;
    private final String message;

    public TokenTestResult(String rawResponse, String token, String message) {
        this.rawResponse = rawResponse;
        this.token = token;
        this.message = message;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}
