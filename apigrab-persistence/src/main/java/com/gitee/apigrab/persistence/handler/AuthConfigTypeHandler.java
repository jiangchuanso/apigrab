package com.gitee.apigrab.persistence.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gitee.apigrab.persistence.entity.AuthConfig;

public class AuthConfigTypeHandler extends JacksonTypeHandler<AuthConfig> {

    @Override
    protected TypeReference<AuthConfig> type() {
        return new TypeReference<AuthConfig>() {};
    }
}
