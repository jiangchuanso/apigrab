package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.AuthConfigRequest;
import com.gitee.apigrab.admin.service.AuthConfigService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.persistence.entity.AuthConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthConfigControllerTest {

    private final AuthConfigService mockService = mock(AuthConfigService.class);
    private final AuthConfigController controller = new AuthConfigController();

    private void injectService() throws Exception {
        java.lang.reflect.Field field = AuthConfigController.class.getDeclaredField("authConfigService");
        field.setAccessible(true);
        field.set(controller, mockService);
    }

    @Test
    void testGetById() throws Exception {
        injectService();
        AuthConfigEntity entity = new AuthConfigEntity();
        entity.setId(1L);
        entity.setName("共享Bearer");
        when(mockService.getById(1L)).thenReturn(entity);

        ResultEntity<AuthConfigEntity> result = controller.getById(1L);
        assertTrue(result.isSuccess());
        assertEquals("共享Bearer", result.getData().getName());
    }

    @Test
    void testCreate() throws Exception {
        injectService();
        AuthConfigRequest request = new AuthConfigRequest();
        request.setName("新认证");
        request.setAuthType(AuthType.BASIC);

        ResultEntity<Void> result = controller.create(request);
        assertTrue(result.isSuccess());
        ArgumentCaptor<AuthConfigEntity> captor = ArgumentCaptor.forClass(AuthConfigEntity.class);
        verify(mockService).create(captor.capture());
        assertEquals("新认证", captor.getValue().getName());
        assertEquals(AuthType.BASIC, captor.getValue().getAuthType());
    }

    @Test
    void testUpdate() throws Exception {
        injectService();
        AuthConfigRequest request = new AuthConfigRequest();
        request.setName("更新认证");
        request.setAuthType(AuthType.BEARER);

        ResultEntity<Void> result = controller.update(1L, request);
        assertTrue(result.isSuccess());
        ArgumentCaptor<AuthConfigEntity> captor = ArgumentCaptor.forClass(AuthConfigEntity.class);
        verify(mockService).update(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals("更新认证", captor.getValue().getName());
    }

    @Test
    void testDelete() throws Exception {
        injectService();
        ResultEntity<Void> result = controller.delete(1L);
        assertTrue(result.isSuccess());
        verify(mockService).delete(1L);
    }

    @Test
    void testTokenReturnsStructuredResult() throws Exception {
        injectService();
        AuthConfigRequest request = new AuthConfigRequest();
        request.setAuthType(AuthType.OAUTH2);
        TokenTestResult payload = new TokenTestResult("{\"access_token\":\"t\"}", "t", null);
        when(mockService.testToken(any())).thenReturn(payload);

        ResultEntity<TokenTestResult> result = controller.testToken(request);
        assertTrue(result.isSuccess());
        assertEquals("t", result.getData().getToken());
        assertNotNull(result.getData().getRawResponse());
    }
}
