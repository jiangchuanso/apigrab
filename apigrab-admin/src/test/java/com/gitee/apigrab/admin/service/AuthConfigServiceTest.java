package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.core.auth.impl.OAuth2Handler;
import com.gitee.apigrab.core.auth.impl.PasswordTokenAuthHandler;
import com.gitee.apigrab.persistence.dao.AuthConfigDao;
import com.gitee.apigrab.persistence.entity.AuthConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthConfigServiceTest {

    @Mock private AuthConfigDao authConfigDao;
    @Mock private OAuth2Handler oauth2Handler;
    @Mock private PasswordTokenAuthHandler passwordTokenAuthHandler;

    @InjectMocks private AuthConfigService service;

    private AuthConfigEntity entity;

    @BeforeEach
    void setUp() {
        entity = new AuthConfigEntity();
        entity.setId(1L);
        entity.setName("共享Basic认证");
        entity.setAuthType(AuthType.BASIC);
    }

    @Test
    void testGetById() {
        when(authConfigDao.getById(1L)).thenReturn(entity);
        assertEquals("共享Basic认证", service.getById(1L).getName());
    }

    @Test
    void testGetByIdNotFound() {
        when(authConfigDao.getById(999L)).thenReturn(null);
        assertThrows(CommonException.class, () -> service.getById(999L));
    }

    @Test
    void testList() {
        when(authConfigDao.list(any())).thenReturn(Collections.singletonList(entity));
        assertEquals(1, service.list(new AuthConfigEntity()).size());
    }

    @Test
    void testCreateDefaultsEnabled() {
        entity.setStatus(null);
        service.create(entity);
        assertEquals(EnableStatus.ENABLED, entity.getStatus());
        verify(authConfigDao).insert(entity);
    }

    @Test
    void testCreateRejectsBlankName() {
        entity.setName("   ");
        CommonException e = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(e.getMessage().contains("不能为空"));
        verify(authConfigDao, never()).insert(any());
    }

    @Test
    void testCreateRejectsOverlongName() {
        entity.setName(repeat('a', 101));
        CommonException e = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(e.getMessage().contains("长度"));
        verify(authConfigDao, never()).insert(any());
    }

    @Test
    void testCreateRejectsDuplicateName() {
        AuthConfigEntity existing = new AuthConfigEntity();
        existing.setId(99L);
        existing.setName("共享Basic认证");
        when(authConfigDao.getByName("共享Basic认证")).thenReturn(existing);

        CommonException e = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(e.getMessage().contains("已存在"));
        verify(authConfigDao, never()).insert(any());
    }

    @Test
    void testCreateTrimsName() {
        entity.setName("  共享Basic认证  ");
        when(authConfigDao.getByName("共享Basic认证")).thenReturn(null);
        service.create(entity);
        assertEquals("共享Basic认证", entity.getName());
    }

    @Test
    void testCreateRejectsNoneType() {
        entity.setAuthType(AuthType.NONE);
        CommonException e = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(e.getMessage().contains("认证类型"));
        verify(authConfigDao, never()).insert(any());
    }

    @Test
    void testCreateRejectsNullType() {
        entity.setAuthType(null);
        assertThrows(CommonException.class, () -> service.create(entity));
        verify(authConfigDao, never()).insert(any());
    }

    @Test
    void testUpdateAllowsSameNameOnItself() {
        AuthConfigEntity self = new AuthConfigEntity();
        self.setId(1L);
        self.setName("共享Basic认证");
        when(authConfigDao.getByName("共享Basic认证")).thenReturn(self);
        service.update(entity);
        verify(authConfigDao).updateById(entity);
    }

    @Test
    void testUpdateRejectsNameUsedByAnother() {
        AuthConfigEntity other = new AuthConfigEntity();
        other.setId(2L);
        other.setName("共享Basic认证");
        when(authConfigDao.getByName("共享Basic认证")).thenReturn(other);
        CommonException e = assertThrows(CommonException.class, () -> service.update(entity));
        assertTrue(e.getMessage().contains("已存在"));
        verify(authConfigDao, never()).updateById(any());
    }

    @Test
    void testDeleteAllowedWhenNotReferenced() {
        when(authConfigDao.countReferences(1L)).thenReturn(0L);
        service.delete(1L);
        verify(authConfigDao).deleteById(1L);
    }

    @Test
    void testDeleteBlockedWhenReferenced() {
        when(authConfigDao.countReferences(1L)).thenReturn(2L);
        CommonException e = assertThrows(CommonException.class, () -> service.delete(1L));
        assertTrue(e.getMessage().contains("已被接口引用"));
        verify(authConfigDao, never()).deleteById(any());
    }

    // ---------- 获取 Token 测试（design D18）----------

    @Test
    void testTokenRejectsUnsupportedType() {
        entity.setAuthType(AuthType.BASIC);
        CommonException e = assertThrows(CommonException.class, () -> service.testToken(entity));
        assertTrue(e.getMessage().contains("支持"));
    }

    @Test
    void testTokenDelegatesToOAuth2() {
        entity.setAuthType(AuthType.OAUTH2);
        TokenTestResult delegated = new TokenTestResult("{\"access_token\":\"oauth2-token\"}", "oauth2-token", null);
        when(oauth2Handler.fetchTokenForTest(any())).thenReturn(delegated);
        TokenTestResult result = service.testToken(entity);
        assertEquals("oauth2-token", result.getToken());
        assertNotNull(result.getRawResponse());
    }

    @Test
    void testTokenDelegatesToPasswordToken() {
        entity.setAuthType(AuthType.PASSWORD_TOKEN);
        TokenTestResult delegated = new TokenTestResult("{\"data\":{\"token\":\"pwd-token\"}}", "pwd-token", null);
        when(passwordTokenAuthHandler.fetchTokenForTest(any())).thenReturn(delegated);
        TokenTestResult result = service.testToken(entity);
        assertEquals("pwd-token", result.getToken());
    }

    @Test
    void testTokenPassesThroughRawResponseWhenPathBlank() {
        entity.setAuthType(AuthType.PASSWORD_TOKEN);
        TokenTestResult blankPath = new TokenTestResult("{\"data\":{}}", null, "未设置 Token 提取路径");
        when(passwordTokenAuthHandler.fetchTokenForTest(any())).thenReturn(blankPath);
        TokenTestResult result = service.testToken(entity);
        assertNull(result.getToken());
        assertNotNull(result.getRawResponse());
        assertNotNull(result.getMessage());
    }

    @Test
    void testTokenWrapsFetchFailure() {
        entity.setAuthType(AuthType.OAUTH2);
        when(oauth2Handler.fetchTokenForTest(any())).thenThrow(new RuntimeException("boom"));
        CommonException e = assertThrows(CommonException.class, () -> service.testToken(entity));
        assertTrue(e.getMessage().contains("获取 Token 失败"));
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
