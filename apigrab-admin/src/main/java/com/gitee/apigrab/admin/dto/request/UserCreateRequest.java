package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

/**
 * Create-user request.
 */
@Getter
@Setter
public class UserCreateRequest {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String address;
    private UserRole role;
    private EnableStatus status;
}
