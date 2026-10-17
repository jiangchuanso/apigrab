package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Reusable authentication configuration that may be referenced by many api configs
 * (APIGRAB_API_CONFIG.auth_config_id). Distinct from {@link AuthConfig}, which is the JSON payload.
 */
@Getter
@Setter
public class AuthConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private AuthType authType;
    private AuthConfig authConfig;
    private EnableStatus status;
    private String remark;
    private Date createTime;
    private Date updateTime;
    /** Non-persistent: number of api configs referencing this auth config (aggregated on read). */
    private Integer refCount;
}
