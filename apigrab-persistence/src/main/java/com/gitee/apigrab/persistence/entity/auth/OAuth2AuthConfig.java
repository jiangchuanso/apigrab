package com.gitee.apigrab.persistence.entity.auth;

import com.gitee.apigrab.persistence.entity.AuthConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * OAUTH2 auth payload (client credentials): token endpoint + client credentials,
 * optional scope, token extraction path (default {@code $.access_token}) and expiry.
 */
@Getter
@Setter
public class OAuth2AuthConfig extends AuthConfig {
    private static final long serialVersionUID = 1L;
    private String tokenUrl;
    private String clientId;
    private String clientSecret;
    private String scope;
    /** 从 Token 响应中提取 Token 的 JSONPath。空时使用默认 $.access_token。 */
    private String tokenPath;
    /** Token 有效期（秒）。空时回退到响应 expires_in，再回退默认 3600。 */
    private Integer expireSeconds;
}
