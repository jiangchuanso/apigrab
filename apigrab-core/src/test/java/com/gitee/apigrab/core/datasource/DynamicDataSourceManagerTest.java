package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.common.util.PasswordUtils;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicDataSourceManagerTest {

    private DynamicDataSourceManager manager;
    private DatasourceConfigDao dao;

    @BeforeEach
    void setUp() throws Exception {
        manager = new DynamicDataSourceManager();
        dao = Mockito.mock(DatasourceConfigDao.class);
        java.lang.reflect.Field field = DynamicDataSourceManager.class.getDeclaredField("datasourceConfigDao");
        field.setAccessible(true);
        field.set(manager, dao);
    }

    @Test
    void testLazyBuildOnFirstUse() {
        DatasourceConfigEntity config = createMySqlConfig(1L, "测试MySQL");
        when(dao.getById(1L)).thenReturn(config);

        // 按需构建：目标不可达时首次使用抛异常，不会缓存连接池
        assertThrows(Exception.class, () -> manager.getConnection(1L));
        verify(dao).getById(1L);
    }

    @Test
    void testDestroyNonExistentDataSource() {
        // 销毁不存在的数据源，不应抛异常
        assertDoesNotThrow(() -> manager.destroyDataSource(999L));
    }

    @Test
    void testGetConnectionNotFound() {
        // dao 未返回配置（默认 null）→ 按需构建时判定数据源不存在
        RuntimeException ex = assertThrows(RuntimeException.class, () -> manager.getConnection(999L));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testGetDataSourceNotFound() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> manager.getDataSource(999L));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testTransientConnectionWithInvalidTargetReturnsFalse() {
        // 未保存态：id 为空、密码为明文；目标不可达时应返回 false 而非抛出
        DatasourceConfigEntity config = createMySqlConfig(null, "临时试连");
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:1/nonexistent?connectTimeout=300&socketTimeout=300");
        config.setPassword("plainTextPassword");

        assertFalse(manager.testConnection(config));
    }

    private DatasourceConfigEntity createMySqlConfig(Long id, String name) {
        DatasourceConfigEntity config = new DatasourceConfigEntity();
        config.setId(id);
        config.setName(name);
        config.setDbType(DbType.MYSQL);
        config.setJdbcUrl("jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC");
        config.setDriverClass("com.mysql.cj.jdbc.Driver");
        config.setUsername("root");
        config.setPassword(PasswordUtils.encrypt("test123"));
        config.setPoolMaxSize(5);
        config.setPoolMinIdle(1);
        config.setStatus(EnableStatus.ENABLED);
        return config;
    }
}
