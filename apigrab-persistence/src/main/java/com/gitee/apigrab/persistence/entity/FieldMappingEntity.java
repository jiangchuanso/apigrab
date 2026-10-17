package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.TargetType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class FieldMappingEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long taskId;
    private String sourceField;
    private String targetField;
    private TargetType targetType;
    private Integer targetLength;
    private Integer nullable;
    private String defaultValue;
    private String convertExpr;
    private Integer sortOrder;
    private Date createTime;
}
