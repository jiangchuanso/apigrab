package com.gitee.apigrab.persistence.handler;

import com.gitee.apigrab.persistence.entity.enums.TaskStatus;

/**
 * Maps the {@link TaskStatus} enum to/from its integer code (no-arg constructor so MyBatis
 * can instantiate it without inferring a javaType).
 */
public class TaskStatusTypeHandler extends CodedEnumTypeHandler<TaskStatus> {

    public TaskStatusTypeHandler() {
        super(TaskStatus.class);
    }
}
