package com.gitee.apigrab.admin.dto;

import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * User detail exposed to clients (never includes the password).
 */
@Getter
@Setter
public class SystemUserResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String address;
    private UserRole role;
    private EnableStatus status;
    private Date createTime;
    private Date updateTime;

    public static SystemUserResponse from(SystemUserEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemUserResponse response = new SystemUserResponse();
        response.setId(entity.getId());
        response.setUsername(entity.getUsername());
        response.setRealName(entity.getRealName());
        response.setEmail(entity.getEmail());
        response.setAddress(entity.getAddress());
        response.setRole(entity.getRole());
        response.setStatus(entity.getStatus());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }
}
