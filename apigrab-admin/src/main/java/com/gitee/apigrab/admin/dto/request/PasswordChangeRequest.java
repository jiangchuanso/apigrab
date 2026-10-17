package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Change-password request for the current user.
 */
@Getter
@Setter
public class PasswordChangeRequest {
    private String oldPassword;
    private String newPassword;
}
