package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

/**
 * Update-user request (profile/role/status; password is not modified here).
 */
@Getter
@Setter
public class UserUpdateRequest {
    private String realName;
    private String email;
    private String address;
    private UserRole role;
    private EnableStatus status;
}
