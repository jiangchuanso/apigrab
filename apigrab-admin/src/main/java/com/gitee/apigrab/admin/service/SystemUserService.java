package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.admin.dto.LoginResponse;
import com.gitee.apigrab.admin.dto.SystemUserResponse;
import com.gitee.apigrab.admin.dto.request.PasswordChangeRequest;
import com.gitee.apigrab.admin.dto.request.ProfileUpdateRequest;
import com.gitee.apigrab.admin.dto.request.SystemUserListQuery;
import com.gitee.apigrab.admin.dto.request.UserCreateRequest;
import com.gitee.apigrab.admin.dto.request.UserResetPasswordRequest;
import com.gitee.apigrab.admin.dto.request.UserUpdateRequest;
import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.util.BCryptUtils;
import com.gitee.apigrab.persistence.dao.SystemUserDao;
import com.gitee.apigrab.persistence.dao.UserTokenDao;
import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import com.gitee.apigrab.persistence.entity.UserTokenEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class SystemUserService {

    @Resource
    private SystemUserDao systemUserDao;

    @Resource
    private UserTokenDao userTokenDao;

    @Value("${auth.token.expire-seconds:7200}")
    private long expireSeconds = 7200;

    public LoginResponse login(String username, String password) {
        SystemUserEntity user = systemUserDao.getByUsername(username);
        if (user == null || !BCryptUtils.matches(password, user.getPassword())) {
            throw new CommonException(ResponseErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != EnableStatus.ENABLED) {
            throw new CommonException(ResponseErrorCode.UNAUTHORIZED, "账号已被禁用");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        UserTokenEntity tokenEntity = new UserTokenEntity();
        tokenEntity.setToken(token);
        tokenEntity.setUserId(user.getId());
        tokenEntity.setUsername(user.getUsername());
        tokenEntity.setExpireTime(new Date(now.getTime() + expireSeconds * 1000L));
        tokenEntity.setLastAccessTime(now);
        userTokenDao.insert(tokenEntity);
        return new LoginResponse(token, user.getUsername(), user.getRealName(), user.getRole());
    }

    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            userTokenDao.deleteByToken(token);
        }
    }

    /**
     * Validate a token; returns the current user context or null when invalid/expired.
     * Refreshes the sliding expiry on success.
     */
    public CurrentUser validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        UserTokenEntity tokenEntity = userTokenDao.getByToken(token);
        if (tokenEntity == null) {
            return null;
        }
        Date now = new Date();
        if (tokenEntity.getExpireTime() != null && tokenEntity.getExpireTime().before(now)) {
            userTokenDao.deleteByToken(token);
            return null;
        }
        SystemUserEntity user = systemUserDao.getById(tokenEntity.getUserId());
        if (user == null || user.getStatus() != EnableStatus.ENABLED) {
            userTokenDao.deleteByToken(token);
            return null;
        }
        userTokenDao.updateLastAccess(token, now, new Date(now.getTime() + expireSeconds * 1000L));
        return new CurrentUser(user.getId(), user.getUsername(), user.getRole());
    }

    public SystemUserResponse getCurrentUser(Long userId) {
        return SystemUserResponse.from(requireUser(userId));
    }

    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        SystemUserEntity user = requireUser(userId);
        SystemUserEntity update = new SystemUserEntity();
        update.setId(user.getId());
        if (request != null) {
            update.setRealName(request.getRealName());
            update.setEmail(request.getEmail());
            update.setAddress(request.getAddress());
        }
        systemUserDao.updateById(update);
    }

    public void changePassword(Long userId, PasswordChangeRequest request) {
        SystemUserEntity user = requireUser(userId);
        if (request == null || !BCryptUtils.matches(request.getOldPassword(), user.getPassword())) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "旧密码不正确");
        }
        requirePassword(request.getNewPassword());
        systemUserDao.updatePassword(user.getId(), BCryptUtils.hashPassword(request.getNewPassword()));
    }

    public List<SystemUserEntity> list(SystemUserListQuery query) {
        SystemUserEntity probe = new SystemUserEntity();
        probe.setUsername(query.getUsername());
        probe.setRole(query.getRole());
        if (query.getStatus() != null) {
            EnableStatus status = EnableStatus.fromCode(query.getStatus());
            if (status == null) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST, "无效的状态值: " + query.getStatus());
            }
            probe.setStatus(status);
        }
        return systemUserDao.list(probe);
    }

    public SystemUserResponse getById(Long id) {
        return SystemUserResponse.from(requireUser(id));
    }

    public void create(UserCreateRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "用户名不能为空");
        }
        requirePassword(request.getPassword());
        if (systemUserDao.getByUsername(request.getUsername()) != null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "用户名已存在");
        }
        SystemUserEntity user = new SystemUserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(BCryptUtils.hashPassword(request.getPassword()));
        user.setRealName(request.getRealName() == null ? "" : request.getRealName());
        user.setEmail(request.getEmail() == null ? "" : request.getEmail());
        user.setAddress(request.getAddress() == null ? "" : request.getAddress());
        user.setRole(request.getRole() == null ? UserRole.USER : request.getRole());
        user.setStatus(request.getStatus() == null ? EnableStatus.ENABLED : request.getStatus());
        systemUserDao.insert(user);
    }

    public void update(Long id, UserUpdateRequest request) {
        SystemUserEntity user = requireUser(id);
        if (request != null && request.getRole() != null
                && user.getRole() == UserRole.ADMIN && request.getRole() != UserRole.ADMIN) {
            requireAnotherEnabledAdmin(id);
        }
        if (request != null && request.getStatus() != null && request.getStatus() != EnableStatus.ENABLED
                && user.getRole() == UserRole.ADMIN) {
            requireAnotherEnabledAdmin(id);
        }
        SystemUserEntity update = new SystemUserEntity();
        update.setId(user.getId());
        if (request != null) {
            update.setRealName(request.getRealName());
            update.setEmail(request.getEmail());
            update.setAddress(request.getAddress());
            update.setRole(request.getRole());
            update.setStatus(request.getStatus());
        }
        systemUserDao.updateById(update);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        SystemUserEntity user = requireUser(id);
        if (user.getId().equals(currentUserId)) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "不能删除当前登录用户");
        }
        if (user.getRole() == UserRole.ADMIN && user.getStatus() == EnableStatus.ENABLED) {
            requireAnotherEnabledAdmin(id);
        }
        userTokenDao.deleteByUserId(id);
        systemUserDao.deleteById(id);
    }

    public void resetPassword(Long id, UserResetPasswordRequest request) {
        requireUser(id);
        if (request == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "新密码不能为空");
        }
        requirePassword(request.getNewPassword());
        systemUserDao.updatePassword(id, BCryptUtils.hashPassword(request.getNewPassword()));
    }

    @Transactional
    public void updateStatus(Long id, EnableStatus status, Long currentUserId) {
        if (status == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "状态不能为空");
        }
        SystemUserEntity user = requireUser(id);
        if (user.getId().equals(currentUserId) && status != EnableStatus.ENABLED) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "不能禁用当前登录用户");
        }
        if (user.getRole() == UserRole.ADMIN && status != EnableStatus.ENABLED) {
            requireAnotherEnabledAdmin(id);
        }
        SystemUserEntity update = new SystemUserEntity();
        update.setId(user.getId());
        update.setStatus(status);
        systemUserDao.updateById(update);
        if (status != EnableStatus.ENABLED) {
            userTokenDao.deleteByUserId(id);
        }
    }

    private SystemUserEntity requireUser(Long id) {
        SystemUserEntity user = systemUserDao.getById(id);
        if (user == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void requirePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "密码不能为空");
        }
    }

    /** Ensure that excluding {@code excludedId} there remains at least one enabled admin. */
    private void requireAnotherEnabledAdmin(Long excludedId) {
        SystemUserEntity target = systemUserDao.getById(excludedId);
        int enabledAdmins = systemUserDao.countEnabledByRole(UserRole.ADMIN);
        if (target != null && target.getRole() == UserRole.ADMIN && target.getStatus() == EnableStatus.ENABLED) {
            enabledAdmins--;
        }
        if (enabledAdmins <= 0) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "至少保留一个启用状态的管理员");
        }
    }
}
