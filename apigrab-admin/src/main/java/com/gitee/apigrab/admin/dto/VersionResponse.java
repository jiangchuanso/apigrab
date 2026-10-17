package com.gitee.apigrab.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Version response carrying the running application version.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {
    private String version;
}
