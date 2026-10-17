package com.gitee.apigrab.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class UserTokenEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String token;
    private Long userId;
    private String username;
    private Date createTime;
    private Date expireTime;
    private Date lastAccessTime;
}
