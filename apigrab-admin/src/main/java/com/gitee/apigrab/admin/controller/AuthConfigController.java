package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.AuthConfigListQuery;
import com.gitee.apigrab.admin.dto.request.AuthConfigRequest;
import com.gitee.apigrab.admin.service.AuthConfigService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.model.PageResult;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.persistence.entity.AuthConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/auth-configs")
public class AuthConfigController {

    @Resource
    private AuthConfigService authConfigService;

    @GetMapping
    public ResultEntity<PageResult<AuthConfigEntity>> list(AuthConfigListQuery query) {
        AuthConfigEntity probe = new AuthConfigEntity();
        probe.setName(query.getName());
        probe.setAuthType(query.getAuthType());
        if (query.getStatus() != null) {
            EnableStatus status = EnableStatus.fromCode(query.getStatus());
            if (status == null) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST, "无效的状态值: " + query.getStatus());
            }
            probe.setStatus(status);
        }
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<AuthConfigEntity> list = authConfigService.list(probe);
        PageInfo<AuthConfigEntity> pageInfo = new PageInfo<>(list);
        PageResult<AuthConfigEntity> pageResult = new PageResult<>(
            query.getPageNum(), query.getPageSize(), pageInfo.getTotal(), list);
        return ResultEntity.success(pageResult);
    }

    @GetMapping("/{id}")
    public ResultEntity<AuthConfigEntity> getById(@PathVariable Long id) {
        return ResultEntity.success(authConfigService.getById(id));
    }

    @PostMapping
    public ResultEntity<Void> create(@RequestBody AuthConfigRequest request) {
        authConfigService.create(toEntity(request));
        return ResultEntity.success();
    }

    @PutMapping("/{id}")
    public ResultEntity<Void> update(@PathVariable Long id, @RequestBody AuthConfigRequest request) {
        AuthConfigEntity entity = toEntity(request);
        entity.setId(id);
        authConfigService.update(entity);
        return ResultEntity.success();
    }

    @DeleteMapping("/{id}")
    public ResultEntity<Void> delete(@PathVariable Long id) {
        authConfigService.delete(id);
        return ResultEntity.success();
    }

    /**
     * 获取 Token 测试：仅 OAuth2 / 账号密码换 Token。
     * 返回原始 Token 响应（供前端展示结构并点选提取路径）、提取到的 Token（可空）与说明（可空）。
     */
    @PostMapping("/test")
    public ResultEntity<TokenTestResult> testToken(@RequestBody AuthConfigRequest request) {
        return ResultEntity.success(authConfigService.testToken(toEntity(request)));
    }

    private AuthConfigEntity toEntity(AuthConfigRequest request) {
        AuthConfigEntity entity = new AuthConfigEntity();
        if (request != null) {
            BeanUtils.copyProperties(request, entity);
        }
        return entity;
    }
}
