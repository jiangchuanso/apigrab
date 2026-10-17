package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.service.ApiConfigService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gitee.apigrab.admin.dto.request.ApiConfigListQuery;
import com.gitee.apigrab.admin.dto.request.ApiConfigRequest;
import com.gitee.apigrab.admin.dto.request.DebugScriptRequest;
import com.gitee.apigrab.admin.dto.request.DetailTestRequest;
import com.gitee.apigrab.admin.dto.request.FieldMappingInput;
import com.gitee.apigrab.admin.dto.request.PreviewMappingRequest;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.model.PageResult;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configs")
public class ApiConfigController {

    @Resource
    private ApiConfigService apiConfigService;

    @GetMapping
    public ResultEntity<PageResult<ApiConfigEntity>> list(ApiConfigListQuery query) {
        ApiConfigEntity probe = new ApiConfigEntity();
        probe.setName(query.getName());
        if (query.getStatus() != null) {
            EnableStatus status = EnableStatus.fromCode(query.getStatus());
            if (status == null) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST, "无效的状态值: " + query.getStatus());
            }
            probe.setStatus(status);
        }
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<ApiConfigEntity> list = apiConfigService.list(probe);
        PageInfo<ApiConfigEntity> pageInfo = new PageInfo<>(list);
        PageResult<ApiConfigEntity> pageResult = new PageResult<>(
            query.getPageNum(), query.getPageSize(), pageInfo.getTotal(), list);
        return ResultEntity.success(pageResult);
    }

    @GetMapping("/{id}")
    public ResultEntity<ApiConfigEntity> getById(@PathVariable Long id) {
        return ResultEntity.success(apiConfigService.getById(id));
    }

    @PostMapping
    public ResultEntity<Void> create(@RequestBody ApiConfigRequest request) {
        apiConfigService.create(toEntity(request));
        return ResultEntity.success();
    }

    @PutMapping("/{id}")
    public ResultEntity<Void> update(@PathVariable Long id, @RequestBody ApiConfigRequest request) {
        ApiConfigEntity entity = toEntity(request);
        entity.setId(id);
        apiConfigService.update(entity);
        return ResultEntity.success();
    }

    @DeleteMapping("/{id}")
    public ResultEntity<Void> delete(@PathVariable Long id) {
        apiConfigService.delete(id);
        return ResultEntity.success();
    }

    @PostMapping("/test")
    public ResultEntity<Map<String, Object>> testTransient(@RequestBody ApiConfigRequest request) {
        return ResultEntity.success(apiConfigService.testConfig(toEntity(request)));
    }

    @PostMapping("/{id}/test")
    public ResultEntity<Map<String, Object>> test(
            @PathVariable Long id,
            @RequestBody(required = false) ApiConfigRequest request) {
        if (request != null) {
            ApiConfigEntity entity = toEntity(request);
            entity.setId(id);
            return ResultEntity.success(apiConfigService.testConfig(entity));
        }
        return ResultEntity.success(apiConfigService.testApi(id));
    }

    @PostMapping("/{id}/test-detail")
    public ResultEntity<Map<String, Object>> testDetail(
            @PathVariable Long id,
            @RequestBody DetailTestRequest request) {
        ApiConfigEntity base = apiConfigService.getById(id);
        return ResultEntity.success(
            apiConfigService.testDetail(base, request.getDetailConfig(), request.getSampleKey()));
    }

    @PostMapping("/test-detail")
    public ResultEntity<Map<String, Object>> testDetailTransient(@RequestBody DetailTestRequest request) {
        ApiConfigEntity entity = toEntity(request);
        return ResultEntity.success(
            apiConfigService.testDetail(entity, request.getDetailConfig(), request.getSampleKey()));
    }

    @PostMapping("/{id}/debug-script")
    public ResultEntity<List<Map<String, Object>>> debugScript(
            @PathVariable Long id, @RequestBody DebugScriptRequest request) {
        return ResultEntity.success(
            apiConfigService.debugScript(id, request.getScript(), request.getSampleData()));
    }

    @PostMapping("/{id}/preview-mapping")
    public ResultEntity<List<Map<String, Object>>> previewMapping(
            @PathVariable Long id, @RequestBody PreviewMappingRequest request) {
        return ResultEntity.success(
            apiConfigService.previewMapping(id, toMappingEntities(request.getMappings()), request.getSampleData()));
    }

    private ApiConfigEntity toEntity(ApiConfigRequest request) {
        ApiConfigEntity entity = new ApiConfigEntity();
        if (request != null) {
            BeanUtils.copyProperties(request, entity);
        }
        return entity;
    }

    private List<FieldMappingEntity> toMappingEntities(List<FieldMappingInput> mappings) {
        if (mappings == null) {
            return null;
        }
        List<FieldMappingEntity> entities = new ArrayList<>(mappings.size());
        for (FieldMappingInput mapping : mappings) {
            FieldMappingEntity entity = new FieldMappingEntity();
            BeanUtils.copyProperties(mapping, entity);
            entities.add(entity);
        }
        return entities;
    }
}
