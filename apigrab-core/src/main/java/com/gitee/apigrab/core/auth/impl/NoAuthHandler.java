package com.gitee.apigrab.core.auth.impl;

import com.gitee.apigrab.core.auth.AuthHandler;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;
import org.springframework.stereotype.Component;

@Component
public class NoAuthHandler implements AuthHandler {

    @Override
    public boolean supports(AuthType authType) {
        return authType == null || authType == AuthType.NONE;
    }

    @Override
    public Headers processAuth(ApiConfigEntity config) {
        return new Headers.Builder().build();
    }
}
