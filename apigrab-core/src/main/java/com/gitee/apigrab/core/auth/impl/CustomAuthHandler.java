package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.core.auth.AuthHandler;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.auth.CustomAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Custom auth: injects a user-defined set of headers (key/value map). Blank keys are
 * skipped; a null value is treated as an empty string; an empty set injects nothing.
 */
@Component
public class CustomAuthHandler implements AuthHandler {

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.CUSTOM;
    }

    @Override
    public Headers processAuth(ApiConfigEntity config) {
        AuthConfig auth = config.getAuthConfig();
        CustomAuthConfig custom = auth instanceof CustomAuthConfig ? (CustomAuthConfig) auth : null;
        Map<String, String> headers = custom == null ? null : custom.getHeaders();
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (key != null && !key.trim().isEmpty()) {
                    builder.add(key, value == null ? "" : value);
                }
            });
        }
        return builder.build();
    }
}
