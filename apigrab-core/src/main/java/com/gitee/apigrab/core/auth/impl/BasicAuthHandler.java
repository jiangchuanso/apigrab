package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.core.auth.AuthHandler;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.auth.BasicAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class BasicAuthHandler implements AuthHandler {

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.BASIC;
    }

    @Override
    public Headers processAuth(ApiConfigEntity config) {
        AuthConfig auth = config.getAuthConfig();
        BasicAuthConfig basic = auth instanceof BasicAuthConfig ? (BasicAuthConfig) auth : null;
        String username = basic == null || basic.getUsername() == null ? "" : basic.getUsername();
        String password = basic == null || basic.getPassword() == null ? "" : basic.getPassword();
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return new Headers.Builder()
            .add("Authorization", "Basic " + encoded)
            .build();
    }
}
