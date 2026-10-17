package com.gitee.apigrab.admin.security;

import com.gitee.apigrab.persistence.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight authenticated-user context placed on the request by the auth interceptor.
 * Deliberately not a persistence entity (controller input convention).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {
    private Long id;
    private String username;
    private UserRole role;
}
