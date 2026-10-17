package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class DatasourceConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
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
    private Date createTime;
    private Date updateTime;
    /** 只读派生：引用本数据源的任务数（列表查询时聚合，非持久化列）。 */
    private Integer refCount;
}
