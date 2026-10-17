package com.gitee.apigrab.admin.dto;

import com.gitee.apigrab.persistence.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Login response carrying the access token and basic user info.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String realName;
    private UserRole role;
}
