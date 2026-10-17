package com.gitee.apigrab.persistence.entity.auth;

import com.gitee.apigrab.persistence.entity.AuthConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * PASSWORD_TOKEN auth payload (username/password -> token):
 * a POST to {@code tokenUrl} with a user-defined request body (JSON or form), the token
 * extracted by {@code tokenPath} (JSONPath), then placed at {@code tokenLocation}
 * (HEADER/QUERY/BODY) under {@code tokenKey} using {@code tokenValueFormat} (e.g. "Bearer {token}").
 */
@Getter
@Setter
public class PasswordTokenAuthConfig extends AuthConfig {
    private static final long serialVersionUID = 1L;
    private String tokenUrl;
    /** Token 请求体格式：JSON / FORM。 */
    private String requestBodyType;
    /** 用户自定义的 Token 请求体字段（键=字段名，值=字段值）。 */
    private Map<String, String> bodyFields;
    /** 从 Token 响应中提取 Token 的 JSONPath（必填，无默认）。 */
    private String tokenPath;
    /** Token 有效期（秒）。空时回退到响应 expires_in，再回退默认 3600。 */
    private Integer expireSeconds;
    /** Token 放置位置：HEADER / QUERY / BODY（默认 HEADER）。 */
    private String tokenLocation;
    /** Token 放置键名（默认 Authorization）。 */
    private String tokenKey;
    /** Token 值格式模板，含占位符 {token}（默认 "Bearer {token}"）。 */
    private String tokenValueFormat;
}
