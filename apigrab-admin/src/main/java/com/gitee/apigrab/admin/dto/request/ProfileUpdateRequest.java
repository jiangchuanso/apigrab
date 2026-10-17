package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Update request for the current user's own profile.
 */
@Getter
@Setter
public class ProfileUpdateRequest {
    private String realName;
    private String email;
    private String address;
}
