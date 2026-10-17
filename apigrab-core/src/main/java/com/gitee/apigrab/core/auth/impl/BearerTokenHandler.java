package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.core.auth.AuthHandler;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.auth.BearerAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenHandler implements AuthHandler {

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.BEARER;
    }

    @Override
    public Headers processAuth(ApiConfigEntity config) {
        AuthConfig auth = config.getAuthConfig();
        BearerAuthConfig bearer = auth instanceof BearerAuthConfig ? (BearerAuthConfig) auth : null;
        String token = bearer == null || bearer.getToken() == null ? "" : bearer.getToken();
        return new Headers.Builder()
            .add("Authorization", "Bearer " + token)
            .build();
    }
}
