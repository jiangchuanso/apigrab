package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.ScheduleType;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import lombok.Getter;
import lombok.Setter;

/**
 * Writable fields for creating/updating a collect task. Mirrors the writable
 * fields of {@code CollectTaskEntity} so that BeanUtils.copyProperties can map
 * it directly, keeping the request JSON shape unchanged.
 */
@Getter
@Setter
public class TaskInput {
    private String name;
    private Long apiConfigId;
    private Long datasourceId;
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
}
