package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.AuthType;
import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for the auth config list endpoint. The coded enum {@code status}
 * is bound as an integer code (0/1) to avoid name-based enum conversion on query strings.
 */
@Getter
@Setter
public class AuthConfigListQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String name;
    private AuthType authType;
    private Integer status;
}
