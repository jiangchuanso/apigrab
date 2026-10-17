package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.SystemUserResponse;
import com.gitee.apigrab.admin.dto.request.SystemUserListQuery;
import com.gitee.apigrab.admin.dto.request.UserCreateRequest;
import com.gitee.apigrab.admin.dto.request.UserResetPasswordRequest;
import com.gitee.apigrab.admin.dto.request.UserStatusRequest;
import com.gitee.apigrab.admin.dto.request.UserUpdateRequest;
import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.admin.service.SystemUserService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemUserControllerTest {

    private SystemUserController newController(SystemUserService mockService) throws Exception {
        SystemUserController controller = new SystemUserController();
        java.lang.reflect.Field field = SystemUserController.class.getDeclaredField("systemUserService");
        field.setAccessible(true);
        field.set(controller, mockService);
        return controller;
    }

    @Test
    void listDelegates() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        SystemUserController controller = newController(mockService);
        SystemUserEntity user = new SystemUserEntity();
        user.setUsername("admin");
        when(mockService.list(any())).thenReturn(Collections.singletonList(user));

        ResultEntity<?> result = controller.list(new SystemUserListQuery());

        assertTrue(result.isSuccess());
        verify(mockService).list(any());
    }

    @Test
    void getByIdDelegates() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        SystemUserController controller = newController(mockService);
        SystemUserResponse user = new SystemUserResponse();
        user.setUsername("admin");
        when(mockService.getById(1L)).thenReturn(user);

        ResultEntity<SystemUserResponse> result = controller.getById(1L);

        assertEquals("admin", result.getData().getUsername());
    }

    @Test
    void createDelegates() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        SystemUserController controller = newController(mockService);
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("u");
        request.setPassword("p");

        controller.create(request);

        verify(mockService).create(request);
    }

    @Test
    void updateDelegates() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        SystemUserController controller = newController(mockService);
        UserUpdateRequest request = new UserUpdateRequest();

        controller.update(5L, request);

        verify(mockService).update(5L, request);
    }

    @Test
    void deleteUsesCurrentUserId() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        SystemUserController controller = newController(mockService);

        controller.delete(5L, new CurrentUser(1L, "admin", UserRole.ADMIN));

        verify(mockService).delete(5L, 1L);
    }

    @Test
    void resetPasswordDelegates() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        SystemUserController controller = newController(mockService);
        UserResetPasswordRequest request = new UserResetPasswordRequest();
        request.setNewPassword("new");

        controller.resetPassword(5L, request);

        verify(mockService).resetPassword(5L, request);
    }

    @Test
    void updateStatusUsesCurrentUserId() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        SystemUserController controller = newController(mockService);
        UserStatusRequest request = new UserStatusRequest();
        request.setStatus(EnableStatus.DISABLED);

        controller.updateStatus(5L, request, new CurrentUser(1L, "admin", UserRole.ADMIN));

        verify(mockService).updateStatus(5L, EnableStatus.DISABLED, 1L);
    }
}
