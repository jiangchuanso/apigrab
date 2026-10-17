package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.admin.dto.LoginResponse;
import com.gitee.apigrab.admin.dto.request.PasswordChangeRequest;
import com.gitee.apigrab.admin.dto.request.ProfileUpdateRequest;
import com.gitee.apigrab.admin.dto.request.SystemUserListQuery;
import com.gitee.apigrab.admin.dto.request.UserCreateRequest;
import com.gitee.apigrab.admin.dto.request.UserResetPasswordRequest;
import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.util.BCryptUtils;
import com.gitee.apigrab.persistence.dao.SystemUserDao;
import com.gitee.apigrab.persistence.dao.UserTokenDao;
import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import com.gitee.apigrab.persistence.entity.UserTokenEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

    @Mock private SystemUserDao systemUserDao;
    @Mock private UserTokenDao userTokenDao;

    @InjectMocks private SystemUserService service;

    private SystemUserEntity admin;

    @BeforeEach
    void setUp() {
        admin = new SystemUserEntity();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword(BCryptUtils.hashPassword("admin123"));
        admin.setRealName("管理员");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(EnableStatus.ENABLED);
    }

    @Test
    void loginSucceeds() {
        when(systemUserDao.getByUsername("admin")).thenReturn(admin);

        LoginResponse response = service.login("admin", "admin123");

        assertEquals("admin", response.getUsername());
        assertEquals(UserRole.ADMIN, response.getRole());
        assertNotNull(response.getToken());
        verify(userTokenDao).insert(any(UserTokenEntity.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        when(systemUserDao.getByUsername("admin")).thenReturn(admin);
        assertThrows(CommonException.class, () -> service.login("admin", "wrong"));
        verify(userTokenDao, never()).insert(any());
    }

    @Test
    void loginRejectsDisabledUser() {
        admin.setStatus(EnableStatus.DISABLED);
        when(systemUserDao.getByUsername("admin")).thenReturn(admin);
        assertThrows(CommonException.class, () -> service.login("admin", "admin123"));
    }

    @Test
    void logoutDeletesToken() {
        service.logout("tok");
        verify(userTokenDao).deleteByToken("tok");
    }

    @Test
    void validateTokenReturnsCurrentUser() {
        UserTokenEntity token = new UserTokenEntity();
        token.setToken("tok");
        token.setUserId(1L);
        token.setExpireTime(new Date(System.currentTimeMillis() + 100000));
        when(userTokenDao.getByToken("tok")).thenReturn(token);
        when(systemUserDao.getById(1L)).thenReturn(admin);

        CurrentUser current = service.validateToken("tok");

        assertNotNull(current);
        assertEquals("admin", current.getUsername());
        assertEquals(UserRole.ADMIN, current.getRole());
        verify(userTokenDao).updateLastAccess(eq("tok"), any(Date.class), any(Date.class));
    }

    @Test
    void validateTokenRejectsExpired() {
        UserTokenEntity token = new UserTokenEntity();
        token.setToken("tok");
        token.setUserId(1L);
        token.setExpireTime(new Date(System.currentTimeMillis() - 1000));
        when(userTokenDao.getByToken("tok")).thenReturn(token);

        assertNull(service.validateToken("tok"));
        verify(userTokenDao).deleteByToken("tok");
    }

    @Test
    void validateTokenRejectsUnknown() {
        when(userTokenDao.getByToken("tok")).thenReturn(null);
        assertNull(service.validateToken("tok"));
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("newpass");

        assertThrows(CommonException.class, () -> service.changePassword(1L, request));
        verify(systemUserDao, never()).updatePassword(any(), any());
    }

    @Test
    void changePasswordUpdatesHash() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("admin123");
        request.setNewPassword("newpass");

        service.changePassword(1L, request);

        verify(systemUserDao).updatePassword(eq(1L), anyString());
    }

    @Test
    void updateProfileCopiesFields() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setRealName("新名字");
        request.setEmail("a@b.com");
        request.setAddress("addr");

        service.updateProfile(1L, request);

        verify(systemUserDao).updateById(any(SystemUserEntity.class));
    }

    @Test
    void listConvertsStatusFilter() {
        when(systemUserDao.list(any())).thenReturn(Collections.singletonList(admin));
        SystemUserListQuery query = new SystemUserListQuery();
        query.setStatus(1);

        java.util.List<SystemUserEntity> result = service.list(query);

        assertEquals(1, result.size());
        org.mockito.ArgumentCaptor<SystemUserEntity> captor = org.mockito.ArgumentCaptor.forClass(SystemUserEntity.class);
        verify(systemUserDao).list(captor.capture());
        assertEquals(EnableStatus.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void listRejectsInvalidStatus() {
        SystemUserListQuery query = new SystemUserListQuery();
        query.setStatus(9);
        assertThrows(CommonException.class, () -> service.list(query));
    }

    @Test
    void createRejectsDuplicateUsername() {
        when(systemUserDao.getByUsername("admin")).thenReturn(admin);
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("admin");
        request.setPassword("x");

        assertThrows(CommonException.class, () -> service.create(request));
        verify(systemUserDao, never()).insert(any());
    }

    @Test
    void createHashesPassword() {
        when(systemUserDao.getByUsername("u1")).thenReturn(null);
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("u1");
        request.setPassword("pass");

        service.create(request);

        verify(systemUserDao).insert(any(SystemUserEntity.class));
    }

    @Test
    void deleteRejectsSelf() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        assertThrows(CommonException.class, () -> service.delete(1L, 1L));
        verify(systemUserDao, never()).deleteById(any());
    }

    @Test
    void deleteRejectsLastEnabledAdmin() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        when(systemUserDao.countEnabledByRole(UserRole.ADMIN)).thenReturn(1);
        assertThrows(CommonException.class, () -> service.delete(1L, 2L));
        verify(systemUserDao, never()).deleteById(any());
    }

    @Test
    void deleteSucceedsWhenAnotherAdminRemains() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        when(systemUserDao.countEnabledByRole(UserRole.ADMIN)).thenReturn(2);

        service.delete(1L, 2L);

        verify(userTokenDao).deleteByUserId(1L);
        verify(systemUserDao).deleteById(1L);
    }

    @Test
    void updateStatusRejectsSelfDisable() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        assertThrows(CommonException.class, () -> service.updateStatus(1L, EnableStatus.DISABLED, 1L));
    }

    @Test
    void resetPasswordUpdatesHash() {
        when(systemUserDao.getById(1L)).thenReturn(admin);
        UserResetPasswordRequest request = new UserResetPasswordRequest();
        request.setNewPassword("newpass");

        service.resetPassword(1L, request);

        verify(systemUserDao).updatePassword(eq(1L), anyString());
    }
}
