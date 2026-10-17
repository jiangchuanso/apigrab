package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Admin reset-password request.
 */
@Getter
@Setter
public class UserResetPasswordRequest {
    private String newPassword;
}
