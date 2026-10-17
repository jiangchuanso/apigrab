package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.DatasourceListQuery;
import com.gitee.apigrab.admin.dto.request.DatasourceRequest;
import com.gitee.apigrab.admin.service.DatasourceService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.model.PageResult;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasources")
public class DatasourceController {

    @Resource
    private DatasourceService datasourceService;

    @GetMapping
    public ResultEntity<PageResult<DatasourceConfigEntity>> list(DatasourceListQuery query) {
        DatasourceConfigEntity probe = new DatasourceConfigEntity();
        probe.setName(query.getName());
        probe.setDbType(query.getDbType());
        if (query.getStatus() != null) {
            EnableStatus status = EnableStatus.fromCode(query.getStatus());
            if (status == null) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST, "无效的状态值: " + query.getStatus());
            }
            probe.setStatus(status);
        }
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<DatasourceConfigEntity> list = datasourceService.list(probe);
        PageInfo<DatasourceConfigEntity> pageInfo = new PageInfo<>(list);
        PageResult<DatasourceConfigEntity> pageResult = new PageResult<>(
            query.getPageNum(), query.getPageSize(), pageInfo.getTotal(), list);
        return ResultEntity.success(pageResult);
    }

    @GetMapping("/{id}")
    public ResultEntity<DatasourceConfigEntity> getById(@PathVariable Long id) {
        return ResultEntity.success(datasourceService.getById(id));
    }

    @PostMapping
    public ResultEntity<Void> create(@RequestBody DatasourceRequest request) {
        datasourceService.create(toEntity(request));
        return ResultEntity.success();
    }

    @PutMapping("/{id}")
    public ResultEntity<Void> update(@PathVariable Long id, @RequestBody DatasourceRequest request) {
        DatasourceConfigEntity entity = toEntity(request);
        entity.setId(id);
        datasourceService.update(entity);
        return ResultEntity.success();
    }

    @DeleteMapping("/{id}")
    public ResultEntity<Void> delete(@PathVariable Long id) {
        datasourceService.delete(id);
        return ResultEntity.success();
    }

    @PostMapping("/{id}/test")
    public ResultEntity<String> testConnection(@PathVariable Long id) {
        boolean ok = datasourceService.testConnection(id);
        return ok ? ResultEntity.success("连接成功") : ResultEntity.error(2001, "连接失败");
    }

    @PostMapping("/test")
    public ResultEntity<String> testConnectionTransient(@RequestBody DatasourceRequest request) {
        boolean ok = datasourceService.testConnection(toEntity(request));
        return ok ? ResultEntity.success("连接成功") : ResultEntity.error(2001, "连接失败");
    }

    @GetMapping("/{id}/schemas")
    public ResultEntity<List<String>> getSchemas(@PathVariable Long id) {
        return ResultEntity.success(datasourceService.getSchemas(id));
    }

    @GetMapping("/{id}/tables")
    public ResultEntity<List<String>> getTables(
            @PathVariable Long id, @RequestParam String schema) {
        return ResultEntity.success(datasourceService.getTables(id, schema));
    }

    @GetMapping("/{id}/columns")
    public ResultEntity<List<Map<String, String>>> getColumns(
            @PathVariable Long id,
            @RequestParam String schema,
            @RequestParam String tableName) {
        return ResultEntity.success(datasourceService.getColumns(id, schema, tableName));
    }

    private DatasourceConfigEntity toEntity(DatasourceRequest request) {
        DatasourceConfigEntity entity = new DatasourceConfigEntity();
        if (request != null) {
            BeanUtils.copyProperties(request, entity);
        }
        return entity;
    }
}
