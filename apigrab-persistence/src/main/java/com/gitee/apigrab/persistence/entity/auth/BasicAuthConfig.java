package com.gitee.apigrab.persistence.entity.auth;

import com.gitee.apigrab.persistence.entity.AuthConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * BASIC auth payload (HTTP Basic): username + password.
 */
@Getter
@Setter
public class BasicAuthConfig extends AuthConfig {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
}
