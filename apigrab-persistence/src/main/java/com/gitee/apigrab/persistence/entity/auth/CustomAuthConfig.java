package com.gitee.apigrab.persistence.entity.auth;

import com.gitee.apigrab.persistence.entity.AuthConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * CUSTOM auth payload: a user-defined set of request headers (key/value map, keys unique).
 */
@Getter
@Setter
public class CustomAuthConfig extends AuthConfig {
    private static final long serialVersionUID = 1L;
    private Map<String, String> headers;
}
