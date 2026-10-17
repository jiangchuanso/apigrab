package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.TaskDetailResponse;
import com.gitee.apigrab.admin.dto.TaskExecuteResponse;
import com.gitee.apigrab.admin.dto.TaskSaveRequest;
import com.gitee.apigrab.admin.dto.request.TaskListQuery;
import com.gitee.apigrab.admin.service.CollectTaskService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.model.PageResult;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class CollectTaskController {

    @Resource
    private CollectTaskService collectTaskService;

    @GetMapping
    public ResultEntity<PageResult<CollectTaskEntity>> list(TaskListQuery query) {
        CollectTaskEntity probe = new CollectTaskEntity();
        probe.setName(query.getName());
        probe.setDatasourceId(query.getDatasourceId());
        probe.setWriteMode(query.getWriteMode());
        probe.setScheduleType(query.getScheduleType());
        if (query.getStatus() != null) {
            TaskStatus status = TaskStatus.fromCode(query.getStatus());
            if (status == null) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST, "无效的状态值: " + query.getStatus());
            }
            probe.setStatus(status);
        }
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<CollectTaskEntity> list = collectTaskService.list(probe);
        PageInfo<CollectTaskEntity> pageInfo = new PageInfo<>(list);
        PageResult<CollectTaskEntity> pageResult = new PageResult<>(
            query.getPageNum(), query.getPageSize(), pageInfo.getTotal(), list);
        return ResultEntity.success(pageResult);
    }

    @GetMapping("/{id}")
    public ResultEntity<TaskDetailResponse> getById(@PathVariable Long id) {
        return ResultEntity.success(collectTaskService.getDetail(id));
    }

    @PostMapping
    public ResultEntity<Void> create(@RequestBody TaskSaveRequest request) {
        collectTaskService.createFromRequest(request);
        return ResultEntity.success();
    }

    @PutMapping("/{id}")
    public ResultEntity<Void> update(@PathVariable Long id, @RequestBody TaskSaveRequest request) {
        collectTaskService.updateFromRequest(id, request);
        return ResultEntity.success();
    }

    @DeleteMapping("/{id}")
    public ResultEntity<Void> delete(@PathVariable Long id) {
        collectTaskService.delete(id);
        return ResultEntity.success();
    }

    @PostMapping("/{id}/execute")
    public ResultEntity<TaskExecuteResponse> execute(@PathVariable Long id) {
        Long executionId = collectTaskService.executeAsync(id);
        return ResultEntity.success(new TaskExecuteResponse(executionId));
    }

    @PutMapping("/{id}/pause")
    public ResultEntity<Void> pause(@PathVariable Long id) {
        collectTaskService.pause(id);
        return ResultEntity.success();
    }

    @PutMapping("/{id}/resume")
    public ResultEntity<Void> resume(@PathVariable Long id) {
        collectTaskService.resume(id);
        return ResultEntity.success();
    }

    @GetMapping("/{id}/preview")
    public ResultEntity<List<Map<String, Object>>> preview(@PathVariable Long id) {
        return ResultEntity.success(collectTaskService.preview(id));
    }
}
