package com.gitee.apigrab.admin.interceptor;

import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.admin.security.TokenResolver;
import com.gitee.apigrab.admin.service.SystemUserService;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Validates the access token for protected {@code /api/**} endpoints and enforces
 * that user-management endpoints are only reachable by administrators.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    @Resource
    private SystemUserService systemUserService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        CurrentUser currentUser = systemUserService.validateToken(TokenResolver.resolve(request));
        if (currentUser == null) {
            throw new CommonException(ResponseErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        request.setAttribute(CURRENT_USER_ATTRIBUTE, currentUser);

        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/users") && currentUser.getRole() != UserRole.ADMIN) {
            throw new CommonException(ResponseErrorCode.FORBIDDEN, "无权限访问");
        }
        return true;
    }
}
