package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for the execution log list endpoint. {@code status} is a plain
 * enum bound by name (e.g. RUNNING/SUCCESS/FAILED/PARTIAL).
 */
@Getter
@Setter
public class ExecutionLogListQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private Long taskId;
    private String taskName;
    private ExecutionStatus status;
}
