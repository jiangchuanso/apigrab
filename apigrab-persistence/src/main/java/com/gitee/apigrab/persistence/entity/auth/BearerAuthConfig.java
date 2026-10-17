package com.gitee.apigrab.persistence.entity.auth;

import com.gitee.apigrab.persistence.entity.AuthConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * BEARER auth payload: a static bearer token.
 */
@Getter
@Setter
public class BearerAuthConfig extends AuthConfig {
    private static final long serialVersionUID = 1L;
    private String token;
}
