package com.gitee.apigrab.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for triggering a manual task execution: the id of the execution record
 * created for this trigger, so the caller can navigate to its detail page.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecuteResponse {
    private Long executionId;
}
