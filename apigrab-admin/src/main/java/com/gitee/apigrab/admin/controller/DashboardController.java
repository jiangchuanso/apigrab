package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.service.DashboardService;
import com.gitee.apigrab.common.model.ResultEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @GetMapping("/statistics")
    public ResultEntity<Map<String, Object>> statistics() {
        return ResultEntity.success(dashboardService.getStatistics());
    }
}
