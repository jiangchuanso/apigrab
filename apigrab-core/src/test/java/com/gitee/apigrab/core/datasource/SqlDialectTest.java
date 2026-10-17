package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlDialectTest {

    private final List<String> columns = Arrays.asList("user_id", "user_name");
    private final List<String> keys = Collections.singletonList("user_id");

    @Test
    void testResolveStrategyPerDatabase() {
        assertTrue(SqlDialect.resolve(DbType.MYSQL) instanceof MySqlDialectStrategy);
        assertTrue(SqlDialect.resolve(DbType.POSTGRESQL) instanceof PostgreSqlDialectStrategy);
        assertTrue(SqlDialect.resolve(DbType.SQLSERVER) instanceof SqlServerDialectStrategy);
        assertTrue(SqlDialect.resolve(DbType.ORACLE) instanceof OracleDialectStrategy);
        // Unknown/empty falls back to MySQL
        assertTrue(SqlDialect.resolve(null) instanceof MySqlDialectStrategy);
        assertTrue(SqlDialect.resolve((DbType) null) instanceof MySqlDialectStrategy);
    }

    @Test
    void testConnectionTestQueriesPerDialect() {
        assertEquals("SELECT 1", SqlDialect.resolve(DbType.MYSQL).getTestQuery());
        assertEquals("SELECT 1", SqlDialect.resolve(DbType.POSTGRESQL).getTestQuery());
        assertEquals("SELECT 1", SqlDialect.resolve(DbType.SQLSERVER).getTestQuery());
        assertEquals("SELECT 1 FROM DUAL", SqlDialect.resolve(DbType.ORACLE).getTestQuery());
    }

    @Test
    void testCatalogScopePerDialect() {
        assertTrue(SqlDialect.resolve(DbType.MYSQL).isCatalogScope());
        assertFalse(SqlDialect.resolve(DbType.POSTGRESQL).isCatalogScope());
        assertFalse(SqlDialect.resolve(DbType.SQLSERVER).isCatalogScope());
        assertFalse(SqlDialect.resolve(DbType.ORACLE).isCatalogScope());
    }

    @Test
    void testSchemaListQueriesPerDialect() {
        assertTrue(SqlDialect.resolve(DbType.MYSQL).getSchemaListQuery().contains("information_schema.schemata"));
        assertTrue(SqlDialect.resolve(DbType.POSTGRESQL).getSchemaListQuery().contains("information_schema.schemata"));
        assertEquals("SELECT name FROM sys.schemas", SqlDialect.resolve(DbType.SQLSERVER).getSchemaListQuery());
        assertEquals("SELECT username FROM all_users", SqlDialect.resolve(DbType.ORACLE).getSchemaListQuery());
    }

    @Test
    void testQuotePerDatabase() {
        assertEquals("`t`", SqlDialect.quote("t", DbType.MYSQL));
        assertEquals("\"t\"", SqlDialect.quote("t", DbType.POSTGRESQL));
        assertEquals("\"t\"", SqlDialect.quote("t", DbType.ORACLE));
        assertEquals("[t]", SqlDialect.quote("t", DbType.SQLSERVER));
    }

    @Test
    void testQuoteSchemaQualified() {
        assertEquals("`db`.`t`", SqlDialect.quote("db.t", DbType.MYSQL));
        assertEquals("\"db\".\"t\"", SqlDialect.quote("db.t", DbType.POSTGRESQL));
    }

    @Test
    void testTruncateSql() {
        assertEquals("TRUNCATE TABLE `t`", SqlDialect.truncateSql("t", DbType.MYSQL));
        assertEquals("TRUNCATE TABLE [t]", SqlDialect.truncateSql("t", DbType.SQLSERVER));
    }

    @Test
    void testMysqlInsertMultiRow() {
        String sql = SqlDialect.buildWriteSql(DbType.MYSQL, WriteMode.INSERT, "t", columns, null, 2);
        assertEquals("INSERT INTO `t` (`user_id`, `user_name`) VALUES (?, ?), (?, ?)", sql);
    }

    @Test
    void testOracleInsertUsesInsertAll() {
        String sql = SqlDialect.buildWriteSql(DbType.ORACLE, WriteMode.INSERT, "t", columns, null, 2);
        assertTrue(sql.startsWith("INSERT ALL"));
        assertTrue(sql.contains("INTO \"t\" (\"user_id\", \"user_name\") VALUES (?, ?)"));
        assertTrue(sql.endsWith("SELECT 1 FROM DUAL"));
        // two INTO clauses
        assertEquals(2, sql.split("INTO ", -1).length - 1);
    }

    @Test
    void testPostgresMergeUsesOnConflict() {
        String sql = SqlDialect.buildWriteSql(DbType.POSTGRESQL, WriteMode.MERGE, "t", columns, keys, 1);
        assertEquals("INSERT INTO \"t\" (\"user_id\", \"user_name\") VALUES (?, ?) "
            + "ON CONFLICT (\"user_id\") DO UPDATE SET \"user_name\" = EXCLUDED.\"user_name\"", sql);
    }

    @Test
    void testMysqlMergeUsesOnDuplicateKey() {
        String sql = SqlDialect.buildWriteSql(DbType.MYSQL, WriteMode.MERGE, "t", columns, keys, 1);
        assertEquals("INSERT INTO `t` (`user_id`, `user_name`) VALUES (?, ?) "
            + "ON DUPLICATE KEY UPDATE `user_name` = VALUES(`user_name`)", sql);
    }

    @Test
    void testSqlServerMerge() {
        String sql = SqlDialect.buildWriteSql(DbType.SQLSERVER, WriteMode.MERGE, "t", columns, keys, 2);
        assertTrue(sql.startsWith("MERGE INTO [t] AS tgt USING (VALUES (?, ?), (?, ?)) AS src ([user_id], [user_name]) ON "));
        assertTrue(sql.contains("ON tgt.[user_id] = src.[user_id]"));
        assertTrue(sql.contains("WHEN MATCHED THEN UPDATE SET tgt.[user_name] = src.[user_name]"));
        assertTrue(sql.contains("WHEN NOT MATCHED THEN INSERT ([user_id], [user_name]) VALUES (src.[user_id], src.[user_name])"));
    }

    @Test
    void testOracleMerge() {
        String sql = SqlDialect.buildWriteSql(DbType.ORACLE, WriteMode.MERGE, "t", columns, keys, 2);
        assertTrue(sql.startsWith("MERGE INTO \"t\" tgt USING (SELECT ? AS \"user_id\", ? AS \"user_name\" FROM DUAL"));
        assertTrue(sql.contains("UNION ALL SELECT ? AS \"user_id\", ? AS \"user_name\" FROM DUAL"));
        assertTrue(sql.contains("ON tgt.\"user_id\" = src.\"user_id\""));
        assertTrue(sql.contains("WHEN NOT MATCHED THEN INSERT (\"user_id\", \"user_name\") VALUES (src.\"user_id\", src.\"user_name\")"));
    }

    @Test
    void testMergeWithoutKeysThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> SqlDialect.buildWriteSql(DbType.MYSQL, WriteMode.MERGE, "t", columns, Collections.emptyList(), 1));
    }

    @Test
    void testTruncateInsertEqualsInsert() {
        String sql = SqlDialect.buildWriteSql(DbType.MYSQL, WriteMode.TRUNCATE_INSERT, "t", columns, null, 1);
        assertEquals("INSERT INTO `t` (`user_id`, `user_name`) VALUES (?, ?)", sql);
    }
}
