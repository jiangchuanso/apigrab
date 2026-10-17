package com.gitee.apigrab.admin.dto;

import com.gitee.apigrab.admin.dto.request.FieldMappingInput;
import com.gitee.apigrab.admin.dto.request.TaskInput;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request for creating/updating a collect task. The task references an existing
 * API config via {@code task.apiConfigId}; field mappings belong to the task.
 */
@Getter
@Setter
public class TaskSaveRequest {
    private TaskInput task;
    private List<FieldMappingInput> mappings;
}
