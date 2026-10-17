package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Writable fields for creating/updating an auth config. Mirrors the writable fields of
 * {@code AuthConfigEntity} (excluding server-managed id/createTime/updateTime/refCount).
 */
@Getter
@Setter
public class AuthConfigRequest {
    private String name;
    private AuthType authType;
    /** Polymorphic payload; JSON must carry the {@code authType} discriminator. */
    private AuthConfig authConfig;
    private EnableStatus status;
    private String remark;
}
