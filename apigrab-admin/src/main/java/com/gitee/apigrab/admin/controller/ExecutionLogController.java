package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.ExecutionLogListQuery;
import com.gitee.apigrab.admin.service.DataPreviewService;
import com.gitee.apigrab.admin.service.ExecutionLogService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gitee.apigrab.common.model.PageResult;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/executions")
public class ExecutionLogController {

    @Resource
    private ExecutionLogService executionLogService;

    @Resource
    private DataPreviewService dataPreviewService;

    @GetMapping
    public ResultEntity<PageResult<ExecutionLogEntity>> list(ExecutionLogListQuery query) {
        ExecutionLogEntity probe = new ExecutionLogEntity();
        probe.setTaskId(query.getTaskId());
        probe.setTaskName(query.getTaskName());
        probe.setStatus(query.getStatus());
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<ExecutionLogEntity> list = executionLogService.list(probe);
        PageInfo<ExecutionLogEntity> pageInfo = new PageInfo<>(list);
        PageResult<ExecutionLogEntity> pageResult = new PageResult<>(
            query.getPageNum(), query.getPageSize(), pageInfo.getTotal(), list);
        return ResultEntity.success(pageResult);
    }

    @GetMapping("/{id}")
    public ResultEntity<ExecutionLogEntity> getById(@PathVariable Long id) {
        return ResultEntity.success(executionLogService.getById(id));
    }

    @GetMapping("/{id}/stages")
    public ResultEntity<List<ExecutionStageLogEntity>> listStages(@PathVariable Long id) {
        return ResultEntity.success(executionLogService.listStages(id));
    }

    @GetMapping("/{id}/data")
    public ResultEntity<List<Map<String, Object>>> previewData(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return ResultEntity.success(dataPreviewService.preview(id, limit));
    }
}
