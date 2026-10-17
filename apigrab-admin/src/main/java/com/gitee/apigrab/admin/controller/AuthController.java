package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.LoginResponse;
import com.gitee.apigrab.admin.dto.SystemUserResponse;
import com.gitee.apigrab.admin.dto.request.LoginRequest;
import com.gitee.apigrab.admin.dto.request.PasswordChangeRequest;
import com.gitee.apigrab.admin.dto.request.ProfileUpdateRequest;
import com.gitee.apigrab.admin.service.SystemUserService;
import com.gitee.apigrab.admin.interceptor.AuthInterceptor;
import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.admin.security.TokenResolver;
import com.gitee.apigrab.common.model.ResultEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 认证控制器 - 登录/登出/当前用户信息/个人资料与密码修改
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private SystemUserService systemUserService;

    @PostMapping("/login")
    public ResultEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResultEntity.success(systemUserService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/logout")
    public ResultEntity<Void> logout(HttpServletRequest request) {
        systemUserService.logout(TokenResolver.resolve(request));
        return ResultEntity.success();
    }

    @GetMapping("/userinfo")
    public ResultEntity<SystemUserResponse> getUserInfo(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) CurrentUser currentUser) {
        return ResultEntity.success(systemUserService.getCurrentUser(currentUser.getId()));
    }

    @PutMapping("/profile")
    public ResultEntity<Void> updateProfile(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) CurrentUser currentUser,
            @RequestBody ProfileUpdateRequest request) {
        systemUserService.updateProfile(currentUser.getId(), request);
        return ResultEntity.success();
    }

    @PutMapping("/password")
    public ResultEntity<Void> changePassword(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) CurrentUser currentUser,
            @RequestBody PasswordChangeRequest request) {
        systemUserService.changePassword(currentUser.getId(), request);
        return ResultEntity.success();
    }
}
