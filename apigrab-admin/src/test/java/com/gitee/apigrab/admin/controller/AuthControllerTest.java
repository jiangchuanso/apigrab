package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.LoginResponse;
import com.gitee.apigrab.admin.dto.SystemUserResponse;
import com.gitee.apigrab.admin.dto.request.LoginRequest;
import com.gitee.apigrab.admin.dto.request.PasswordChangeRequest;
import com.gitee.apigrab.admin.dto.request.ProfileUpdateRequest;
import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.admin.service.SystemUserService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private AuthController newController(SystemUserService mockService) throws Exception {
        AuthController controller = new AuthController();
        java.lang.reflect.Field field = AuthController.class.getDeclaredField("systemUserService");
        field.setAccessible(true);
        field.set(controller, mockService);
        return controller;
    }

    @Test
    void loginReturnsToken() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        AuthController controller = newController(mockService);
        when(mockService.login("admin", "admin123"))
            .thenReturn(new LoginResponse("tok", "admin", "管理员", UserRole.ADMIN));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        ResultEntity<LoginResponse> result = controller.login(request);

        assertTrue(result.isSuccess());
        assertEquals("tok", result.getData().getToken());
        assertEquals(UserRole.ADMIN, result.getData().getRole());
    }

    @Test
    void logoutResolvesToken() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        AuthController controller = newController(mockService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");

        ResultEntity<Void> result = controller.logout(request);

        assertTrue(result.isSuccess());
        verify(mockService).logout("tok");
    }

    @Test
    void userInfoUsesCurrentUser() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        AuthController controller = newController(mockService);
        SystemUserResponse response = new SystemUserResponse();
        response.setUsername("admin");
        when(mockService.getCurrentUser(1L)).thenReturn(response);

        ResultEntity<SystemUserResponse> result = controller.getUserInfo(new CurrentUser(1L, "admin", UserRole.ADMIN));

        assertTrue(result.isSuccess());
        assertEquals("admin", result.getData().getUsername());
    }

    @Test
    void updateProfileDelegatesToCurrentUser() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        AuthController controller = newController(mockService);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setRealName("x");

        controller.updateProfile(new CurrentUser(1L, "admin", UserRole.ADMIN), request);

        verify(mockService).updateProfile(1L, request);
    }

    @Test
    void changePasswordDelegatesToCurrentUser() throws Exception {
        SystemUserService mockService = mock(SystemUserService.class);
        AuthController controller = newController(mockService);
        PasswordChangeRequest request = new PasswordChangeRequest();

        controller.changePassword(new CurrentUser(1L, "admin", UserRole.ADMIN), request);

        verify(mockService).changePassword(1L, request);
    }
}
