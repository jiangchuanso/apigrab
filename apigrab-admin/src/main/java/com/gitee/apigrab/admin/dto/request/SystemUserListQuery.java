package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for the system user list endpoint. {@code status} is the integer
 * code of the coded enum {@code EnableStatus}.
 */
@Getter
@Setter
public class SystemUserListQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String username;
    private UserRole role;
    private Integer status;
}
