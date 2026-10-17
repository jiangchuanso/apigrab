package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Writable fields for saving the alarm config. Mirrors the writable fields of
 * {@code AlarmConfigEntity} so that BeanUtils.copyProperties maps directly.
 */
@Getter
@Setter
public class AlarmConfigRequest {
    private EnableStatus status;
    private String endpoint;
    private String contentType;
    private String inputTemplate;
}
