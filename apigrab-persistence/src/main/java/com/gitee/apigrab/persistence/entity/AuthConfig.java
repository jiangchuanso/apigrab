package com.gitee.apigrab.persistence.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gitee.apigrab.persistence.entity.auth.PasswordTokenAuthConfig;
import com.gitee.apigrab.persistence.entity.auth.BasicAuthConfig;
import com.gitee.apigrab.persistence.entity.auth.BearerAuthConfig;
import com.gitee.apigrab.persistence.entity.auth.CustomAuthConfig;
import com.gitee.apigrab.persistence.entity.auth.OAuth2AuthConfig;

import java.io.Serializable;

/**
 * Authentication payload (persisted as JSON in APIGRAB_AUTH_CONFIG.auth_config).
 *
 * <p>Polymorphic base: the concrete subclass is selected by the {@code authType} property,
 * so each auth type only carries the fields it actually needs.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "authType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicAuthConfig.class, name = "BASIC"),
    @JsonSubTypes.Type(value = BearerAuthConfig.class, name = "BEARER"),
    @JsonSubTypes.Type(value = OAuth2AuthConfig.class, name = "OAUTH2"),
    @JsonSubTypes.Type(value = CustomAuthConfig.class, name = "CUSTOM"),
    @JsonSubTypes.Type(value = PasswordTokenAuthConfig.class, name = "PASSWORD_TOKEN")
})
public abstract class AuthConfig implements Serializable {
    private static final long serialVersionUID = 1L;
}
