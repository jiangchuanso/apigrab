package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.VersionResponse;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.common.util.PomVerUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoint exposing the running application version.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    @GetMapping
    public ResultEntity<VersionResponse> version() {
        return ResultEntity.success(new VersionResponse(PomVerUtils.getCachedProjectVersion()));
    }
}
