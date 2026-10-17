package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.UserRole;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class SystemUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String address;
    private UserRole role;
    private EnableStatus status;
    private Date createTime;
    private Date updateTime;
}
