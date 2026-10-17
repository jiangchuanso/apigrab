package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.TargetType;
import lombok.Getter;
import lombok.Setter;

/**
 * Writable fields for a field mapping. Mirrors the writable fields of
 * {@code FieldMappingEntity} (excluding server-managed id/taskId/createTime).
 */
@Getter
@Setter
public class FieldMappingInput {
    private String sourceField;
    private String targetField;
    private TargetType targetType;
    private Integer targetLength;
    private Integer nullable;
    private String defaultValue;
    private String convertExpr;
    private Integer sortOrder;
}
