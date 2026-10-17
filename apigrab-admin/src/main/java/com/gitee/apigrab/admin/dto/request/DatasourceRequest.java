package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Writable fields for creating/updating or trial-testing a datasource.
 * Mirrors the writable fields of {@code DatasourceConfigEntity} so that
 * BeanUtils.copyProperties maps directly, keeping the request JSON shape unchanged.
 */
@Getter
@Setter
public class DatasourceRequest {
    private String name;
    private DbType dbType;
    private String dbVersion;
    private String jdbcUrl;
    private String driverClass;
    private String driverPath;
    private String username;
    private String password;
    private Integer poolMaxSize;
    private Integer poolMinIdle;
    private Map<String, Object> connProps;
    private EnableStatus status;
    private String remark;
}
