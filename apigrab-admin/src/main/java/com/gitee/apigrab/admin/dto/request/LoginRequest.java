package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Login request body.
 */
@Getter
@Setter
public class LoginRequest {
    private String username;
    private String password;
}
