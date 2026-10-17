package com.gitee.apigrab.admin.interceptor;

import com.gitee.apigrab.admin.security.CurrentUser;
import com.gitee.apigrab.admin.service.SystemUserService;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock private SystemUserService systemUserService;
    @InjectMocks private AuthInterceptor interceptor;

    private MockHttpServletRequest request(String method, String uri, String header) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        if (header != null) {
            request.addHeader("Authorization", header);
        }
        return request;
    }

    @Test
    void rejectsMissingToken() {
        when(systemUserService.validateToken(any())).thenReturn(null);
        MockHttpServletRequest request = request("GET", "/api/tasks", null);

        assertThrows(CommonException.class,
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void allowsOptions() throws Exception {
        MockHttpServletRequest request = request("OPTIONS", "/api/tasks", null);
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void setsCurrentUserForValidToken() throws Exception {
        when(systemUserService.validateToken("tok")).thenReturn(new CurrentUser(1L, "admin", UserRole.ADMIN));
        MockHttpServletRequest request = request("GET", "/api/tasks", "Bearer tok");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertNotNull(request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE));
    }

    @Test
    void forbidsNonAdminOnUserManagement() {
        when(systemUserService.validateToken("tok")).thenReturn(new CurrentUser(2L, "bob", UserRole.USER));
        MockHttpServletRequest request = request("GET", "/api/users", "Bearer tok");

        assertThrows(CommonException.class,
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void allowsAdminOnUserManagement() throws Exception {
        when(systemUserService.validateToken("tok")).thenReturn(new CurrentUser(1L, "admin", UserRole.ADMIN));
        MockHttpServletRequest request = request("GET", "/api/users", "Bearer tok");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
