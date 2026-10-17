package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.core.datasource.DynamicDataSourceManager;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatasourceServiceTest {

    @Mock private DatasourceConfigDao datasourceConfigDao;
    @Mock private DynamicDataSourceManager dataSourceManager;
    @InjectMocks private DatasourceService service;

    private DatasourceConfigEntity entity;

    @BeforeEach
    void setUp() {
        entity = new DatasourceConfigEntity();
        entity.setId(1L);
        entity.setName("测试数据源");
        entity.setDbType(DbType.MYSQL);
        entity.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        entity.setUsername("root");
        entity.setPassword("secret");
    }

    @Test
    void testGetById() {
        when(datasourceConfigDao.getById(1L)).thenReturn(entity);
        DatasourceConfigEntity result = service.getById(1L);
        assertEquals("测试数据源", result.getName());
    }

    @Test
    void testGetByIdNotFound() {
        when(datasourceConfigDao.getById(999L)).thenReturn(null);
        assertThrows(CommonException.class, () -> service.getById(999L));
    }

    @Test
    void testList() {
        when(datasourceConfigDao.list(any())).thenReturn(Collections.singletonList(entity));
        List<DatasourceConfigEntity> list = service.list(new DatasourceConfigEntity());
        assertEquals(1, list.size());
    }

    @Test
    void testCreate() {
        service.create(entity);
        // Password should be encrypted (not the original value)
        assertNotEquals("secret", entity.getPassword());
        verify(datasourceConfigDao, times(1)).insert(entity);
    }

    // ---------- 名称唯一性（design C3/C4/C5/C6）----------
    // 数据源此前已有 uk_name 索引，但缺服务层校验：重名会落到 500 + 原始 SQL 报文。

    @Test
    void testCreateRejectsDuplicateName() {
        DatasourceConfigEntity existing = new DatasourceConfigEntity();
        existing.setId(99L);
        existing.setName("测试数据源");
        when(datasourceConfigDao.getByName("测试数据源")).thenReturn(existing);

        CommonException ex = assertThrows(CommonException.class, () -> service.create(entity));

        assertTrue(ex.getMessage().contains("已存在"), "文案应可读: " + ex.getMessage());
        verify(datasourceConfigDao, never()).insert(any());
    }

    @Test
    void testCreateRejectsBlankName() {
        entity.setName("   ");
        CommonException ex = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(ex.getMessage().contains("不能为空"), "文案应可读: " + ex.getMessage());
        verify(datasourceConfigDao, never()).insert(any());
    }

    @Test
    void testCreateTrimsName() {
        entity.setName("  生产库  ");
        when(datasourceConfigDao.getByName("生产库")).thenReturn(null);

        service.create(entity);

        assertEquals("生产库", entity.getName(), "保存前应去除首尾空白");
        verify(datasourceConfigDao).getByName("生产库");
    }

    /**
     * 判重依赖 SQL 等值查询的结果（C4），不在 Java 层比较字符串。
     * getByName 返回“仅大小写不同”的记录时，服务层应照单判重。
     */
    @Test
    void testCreateRejectsNameMatchedByDatabaseEquality() {
        DatasourceConfigEntity existing = new DatasourceConfigEntity();
        existing.setId(99L);
        existing.setName("ProdDb");
        when(datasourceConfigDao.getByName("PRODDB")).thenReturn(existing);

        entity.setName("PRODDB");
        assertThrows(CommonException.class, () -> service.create(entity));
        verify(datasourceConfigDao, never()).insert(any());
    }

    @Test
    void testUpdateAllowsSameNameOnItself() {
        entity.setId(1L);
        entity.setName("测试数据源");
        DatasourceConfigEntity self = new DatasourceConfigEntity();
        self.setId(1L);
        self.setName("测试数据源");
        when(datasourceConfigDao.getByName("测试数据源")).thenReturn(self);

        service.update(entity);

        verify(datasourceConfigDao, times(1)).updateById(entity);
    }

    @Test
    void testUpdateRejectsNameUsedByAnother() {
        entity.setId(1L);
        entity.setName("测试数据源");
        DatasourceConfigEntity other = new DatasourceConfigEntity();
        other.setId(2L);
        other.setName("测试数据源");
        when(datasourceConfigDao.getByName("测试数据源")).thenReturn(other);

        CommonException ex = assertThrows(CommonException.class, () -> service.update(entity));

        assertTrue(ex.getMessage().contains("已存在"), "文案应可读: " + ex.getMessage());
        verify(datasourceConfigDao, never()).updateById(any());
    }

    // ---------- 名称长度上限（design C10）----------

    @Test
    void testCreateRejectsOverlongName() {
        entity.setName(repeat('a', 101));

        CommonException ex = assertThrows(CommonException.class, () -> service.create(entity));

        assertTrue(ex.getMessage().contains("长度"), "文案应可读: " + ex.getMessage());
        verify(datasourceConfigDao, never()).insert(any());
    }

    @Test
    void testCreateAcceptsNameAtExactLimit() {
        entity.setName(repeat('a', 100));
        when(datasourceConfigDao.getByName(repeat('a', 100))).thenReturn(null);

        service.create(entity);

        verify(datasourceConfigDao, times(1)).insert(entity);
    }

    @Test
    void testUpdateRejectsOverlongName() {
        entity.setId(1L);
        entity.setName(repeat('a', 101));

        assertThrows(CommonException.class, () -> service.update(entity));
        verify(datasourceConfigDao, never()).updateById(any());
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    void testUpdateWithPlainPassword() {
        entity.setPassword("newPass");
        service.update(entity);
        assertFalse(entity.getPassword().startsWith("$ENC$"));
        verify(dataSourceManager, times(1)).destroyDataSource(1L);
    }

    @Test
    void testUpdateWithEncryptedPassword() {
        entity.setPassword("$ENC$encryptedValue");
        service.update(entity);
        assertEquals("$ENC$encryptedValue", entity.getPassword()); // should not re-encrypt
    }

    @Test
    void testDelete() {
        when(datasourceConfigDao.countReferences(1L)).thenReturn(0L);
        service.delete(1L);
        verify(dataSourceManager, times(1)).destroyDataSource(1L);
        verify(datasourceConfigDao, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteBlockedWhenReferencedByTask() {
        when(datasourceConfigDao.countReferences(1L)).thenReturn(2L);
        CommonException ex = assertThrows(CommonException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("已被任务引用"));
        // 被引用时不得销毁连接池、不得删除记录
        verify(dataSourceManager, never()).destroyDataSource(any());
        verify(datasourceConfigDao, never()).deleteById(any());
    }

    @Test
    void testCreateSetsDefaultStatus() {
        entity.setStatus(null);
        service.create(entity);
        assertEquals(EnableStatus.ENABLED, entity.getStatus());
    }

    @Test
    void testTestConnectionUsesDialectQuery() throws Exception {
        when(datasourceConfigDao.getById(1L)).thenReturn(entity);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSourceManager.getConnection(1L)).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.execute("SELECT 1")).thenReturn(true);

        assertTrue(service.testConnection(1L));
        verify(stmt).execute("SELECT 1");
    }

    @Test
    void testTransientConnectionDelegatesToManager() {
        when(dataSourceManager.testConnection(entity)).thenReturn(true);

        assertTrue(service.testConnection(entity));
        verify(dataSourceManager, times(1)).testConnection(entity);
    }

    @Test
    void testGetSchemasUsesDialectQuery() throws Exception {
        when(datasourceConfigDao.getById(1L)).thenReturn(entity);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSourceManager.getConnection(1L)).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery("SELECT schema_name FROM information_schema.schemata")).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(1)).thenReturn("test_db");

        List<String> schemas = service.getSchemas(1L);
        assertEquals(Collections.singletonList("test_db"), schemas);
    }

    @Test
    void testGetTablesUsesDialectQuery() throws Exception {
        when(datasourceConfigDao.getById(1L)).thenReturn(entity);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSourceManager.getConnection(1L)).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery("SHOW FULL TABLES")).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(1)).thenReturn("orders");

        List<String> tables = service.getTables(1L);
        assertEquals(Collections.singletonList("orders"), tables);
    }

    // ---------- 列接口主键元数据（design D2）----------
    // 「关闭自动建表 + 选择已有表」需要知道目标表主键以推导合并主键；
    // 列接口在原有五键之外补一个只读 primaryKey 标记。

    @Test
    void testGetColumnsIncludesPrimaryKeyFlag() throws Exception {
        when(datasourceConfigDao.getById(1L)).thenReturn(entity);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        ResultSet pkRs = mock(ResultSet.class);
        ResultSet colRs = mock(ResultSet.class);
        when(dataSourceManager.getConnection(1L)).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getPrimaryKeys(any(), any(), any())).thenReturn(pkRs);
        when(pkRs.next()).thenReturn(true, false);
        when(pkRs.getString("COLUMN_NAME")).thenReturn("id");
        when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
        when(colRs.next()).thenReturn(true, true, false);
        when(colRs.getString("COLUMN_NAME")).thenReturn("id", "name");
        when(colRs.getString("TYPE_NAME")).thenReturn("BIGINT", "VARCHAR");
        when(colRs.getInt("COLUMN_SIZE")).thenReturn(0, 255);
        when(colRs.getInt("NULLABLE")).thenReturn(0, 1);
        when(colRs.getString("REMARKS")).thenReturn(null, null);

        List<Map<String, String>> columns = service.getColumns(1L, "db1", "t_data");

        assertEquals(2, columns.size());
        assertEquals("id", columns.get(0).get("name"));
        assertEquals("true", columns.get(0).get("primaryKey"), "主键列应标记为 true");
        assertEquals("name", columns.get(1).get("name"));
        assertEquals("false", columns.get(1).get("primaryKey"), "非主键列应标记为 false");
        // 既有五键保持存在
        assertEquals("BIGINT", columns.get(0).get("type"));
        assertEquals("255", columns.get(1).get("size"));
        assertEquals("1", columns.get(1).get("nullable"));
    }

    @Test
    void testGetColumnsWithoutPrimaryKeyMarksAllFalse() throws Exception {
        when(datasourceConfigDao.getById(1L)).thenReturn(entity);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        ResultSet pkRs = mock(ResultSet.class);
        ResultSet colRs = mock(ResultSet.class);
        when(dataSourceManager.getConnection(1L)).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getPrimaryKeys(any(), any(), any())).thenReturn(pkRs);
        when(pkRs.next()).thenReturn(false);
        when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
        when(colRs.next()).thenReturn(true, false);
        when(colRs.getString("COLUMN_NAME")).thenReturn("name");
        when(colRs.getString("TYPE_NAME")).thenReturn("VARCHAR");
        when(colRs.getInt("COLUMN_SIZE")).thenReturn(255);
        when(colRs.getInt("NULLABLE")).thenReturn(1);
        when(colRs.getString("REMARKS")).thenReturn(null);

        List<Map<String, String>> columns = service.getColumns(1L, "db1", "t_data");

        assertEquals(1, columns.size());
        assertEquals("false", columns.get(0).get("primaryKey"));
    }

    @Test
    void testGetColumnsSurvivesPrimaryKeyReadFailure() throws Exception {
        when(datasourceConfigDao.getById(1L)).thenReturn(entity);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        ResultSet colRs = mock(ResultSet.class);
        when(dataSourceManager.getConnection(1L)).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getPrimaryKeys(any(), any(), any())).thenThrow(new SQLException("getPrimaryKeys not supported"));
        when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
        when(colRs.next()).thenReturn(true, false);
        when(colRs.getString("COLUMN_NAME")).thenReturn("name");
        when(colRs.getString("TYPE_NAME")).thenReturn("VARCHAR");
        when(colRs.getInt("COLUMN_SIZE")).thenReturn(255);
        when(colRs.getInt("NULLABLE")).thenReturn(1);
        when(colRs.getString("REMARKS")).thenReturn(null);

        // 主键读取失败不得破坏列清单加载（R4）
        List<Map<String, String>> columns = service.getColumns(1L, "db1", "t_data");

        assertEquals(1, columns.size());
        assertEquals("name", columns.get(0).get("name"));
        assertEquals("false", columns.get(0).get("primaryKey"));
    }
}
