package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Enable/disable user request.
 */
@Getter
@Setter
public class UserStatusRequest {
    private EnableStatus status;
}
