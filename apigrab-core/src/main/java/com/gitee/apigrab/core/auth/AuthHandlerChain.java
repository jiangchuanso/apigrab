package com.gitee.apigrab.core.auth;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthHandlerChain {

    private final List<AuthHandler> handlers;

    public AuthHandlerChain(List<AuthHandler> handlers) {
        this.handlers = handlers;
    }

    public AuthContribution handle(ApiConfigEntity config) {
        return handlers.stream()
            .filter(h -> h.supports(config.getAuthType()))
            .findFirst()
            .orElseThrow(() -> new CommonException(ResponseErrorCode.UNSUPPORTED_AUTH_TYPE, "不支持的认证类型: " + config.getAuthType()))
            .contribute(config);
    }
}
