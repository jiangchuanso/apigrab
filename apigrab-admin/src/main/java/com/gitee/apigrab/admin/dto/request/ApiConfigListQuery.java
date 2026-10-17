package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for the API config list endpoint. The coded enum {@code status}
 * is bound as an integer code (0/1) to avoid name-based enum conversion on query strings.
 */
@Getter
@Setter
public class ApiConfigListQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String name;
    private Integer status;
}
