package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.SystemUserResponse;
import com.gitee.apigrab.admin.service.SystemUserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gitee.apigrab.admin.dto.request.SystemUserListQuery;
import com.gitee.apigrab.admin.dto.request.UserCreateRequest;
import com.gitee.apigrab.admin.dto.request.UserResetPasswordRequest;
import com.gitee.apigrab.admin.dto.request.UserStatusRequest;
import com.gitee.apigrab.admin.dto.request.UserUpdateRequest;
import com.gitee.apigrab.admin.interceptor.AuthInterceptor;
import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.common.model.PageResult;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理（仅 ADMIN 可访问，由 AuthInterceptor 校验角色）
 */
@RestController
@RequestMapping("/api/users")
public class SystemUserController {

    @Resource
    private SystemUserService systemUserService;

    @GetMapping
    public ResultEntity<PageResult<SystemUserResponse>> list(SystemUserListQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<SystemUserEntity> users = systemUserService.list(query);
        PageInfo<SystemUserEntity> pageInfo = new PageInfo<>(users);
        List<SystemUserResponse> rows = new ArrayList<>();
        if (users != null) {
            for (SystemUserEntity user : users) {
                rows.add(SystemUserResponse.from(user));
            }
        }
        PageResult<SystemUserResponse> pageResult = new PageResult<>(
            query.getPageNum(), query.getPageSize(), pageInfo.getTotal(), rows);
        return ResultEntity.success(pageResult);
    }

    @GetMapping("/{id}")
    public ResultEntity<SystemUserResponse> getById(@PathVariable Long id) {
        return ResultEntity.success(systemUserService.getById(id));
    }

    @PostMapping
    public ResultEntity<Void> create(@RequestBody UserCreateRequest request) {
        systemUserService.create(request);
        return ResultEntity.success();
    }

    @PutMapping("/{id}")
    public ResultEntity<Void> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        systemUserService.update(id, request);
        return ResultEntity.success();
    }

    @DeleteMapping("/{id}")
    public ResultEntity<Void> delete(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) CurrentUser currentUser) {
        systemUserService.delete(id, currentUser.getId());
        return ResultEntity.success();
    }

    @PutMapping("/{id}/password")
    public ResultEntity<Void> resetPassword(@PathVariable Long id, @RequestBody UserResetPasswordRequest request) {
        systemUserService.resetPassword(id, request);
        return ResultEntity.success();
    }

    @PutMapping("/{id}/status")
    public ResultEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UserStatusRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) CurrentUser currentUser) {
        systemUserService.updateStatus(id, request.getStatus(), currentUser.getId());
        return ResultEntity.success();
    }
}
