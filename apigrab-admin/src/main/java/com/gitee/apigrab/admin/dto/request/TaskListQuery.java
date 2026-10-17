package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.ScheduleType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for the collect task list endpoint.
 * The coded enum {@code status} is bound as an integer code (0/1/2) to avoid
 * Spring's name-based enum conversion on query strings.
 */
@Getter
@Setter
public class TaskListQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String name;
    private Long datasourceId;
    private WriteMode writeMode;
    private ScheduleType scheduleType;
    private Integer status;
}
