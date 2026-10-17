package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import com.gitee.apigrab.persistence.entity.enums.ScheduleType;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class CollectTaskEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Long apiConfigId;
    private String apiConfigName;
    private Long datasourceId;
    private String datasourceName;
    private String datasourceDbType;
    private String targetSchema;
    private String targetTable;
    private WriteMode writeMode;
    private String mergeKeys;
    private Integer autoCreateTable;
    private Integer batchSize;
    private Integer retryTimes;
    private Long timeoutMs;
    private Integer alarmEnabled;
    private ScheduleType scheduleType;
    private String cronExpression;
    private TaskStatus status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
