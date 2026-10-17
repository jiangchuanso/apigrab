package com.gitee.apigrab.admin.dto;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Composite response for task detail: task + its referenced (read-only) API config + task field mappings.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailResponse {
    private CollectTaskEntity task;
    private ApiConfigEntity apiConfig;
    private List<FieldMappingEntity> mappings;
}
