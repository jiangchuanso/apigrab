package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.TargetType;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlDialectCreateTableTest {

    private FieldMappingEntity field(String targetField, String targetType, Integer length, Integer nullable, String defaultValue) {
        FieldMappingEntity f = new FieldMappingEntity();
        f.setTargetField(targetField);
        f.setTargetType(targetType == null ? null : TargetType.valueOf(targetType));
        f.setTargetLength(length);
        f.setNullable(nullable);
        f.setDefaultValue(defaultValue);
        f.setSortOrder(0);
        return f;
    }

    @Test
    void testGenerateCreateTableSqlSimple() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));
        mappings.add(field("name", "VARCHAR", 100, 1, null));
        mappings.add(field("age", "BIGINT", null, 1, "0"));

        String sql = SqlDialect.generateCreateTableSql("users", mappings, DbType.MYSQL);
        assertTrue(sql.startsWith("CREATE TABLE `users` ("));
        assertTrue(sql.contains("`id` BIGINT NOT NULL"));
        assertTrue(sql.contains("`name` VARCHAR(100)"));
        assertTrue(sql.contains("`age` BIGINT DEFAULT '0'"));
    }

    @Test
    void testGenerateCreateTablePostgres() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));
        mappings.add(field("created_at", "TIMESTAMP", null, 1, null));

        String sql = SqlDialect.generateCreateTableSql("events", mappings, DbType.POSTGRESQL);
        assertTrue(sql.startsWith("CREATE TABLE \"events\" ("));
        assertTrue(sql.contains("\"id\" BIGINT NOT NULL"));
        assertTrue(sql.contains("\"created_at\" TIMESTAMP"));
    }

    @Test
    void testGenerateCreateTableOracle() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));
        mappings.add(field("amount", "DECIMAL", null, 1, null));

        String sql = SqlDialect.generateCreateTableSql("orders", mappings, DbType.ORACLE);
        // Oracle identifiers are double-quoted consistently with DML dialect quoting.
        assertTrue(sql.startsWith("CREATE TABLE \"orders\" ("));
        assertTrue(sql.contains("\"id\" BIGINT"));
        assertTrue(sql.contains("DECIMAL(18,6)"));
    }

    @Test
    void testMapToDbTypeDefaults() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("data", "VARCHAR", null, 1, null));

        String sql = SqlDialect.generateCreateTableSql("test", mappings, DbType.MYSQL);
        assertTrue(sql.contains("VARCHAR(255)"));
    }

    @Test
    void testStringMapsToLongTextPerDialect() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("body", "STRING", null, 1, null));

        // STRING 是长文本，不是 VARCHAR 的别名：各方言使用各自的长文本类型
        assertTrue(SqlDialect.generateCreateTableSql("t", mappings, DbType.MYSQL).contains("`body` LONGTEXT"));
        assertTrue(SqlDialect.generateCreateTableSql("t", mappings, DbType.POSTGRESQL).contains("\"body\" TEXT"));
        assertTrue(SqlDialect.generateCreateTableSql("t", mappings, DbType.ORACLE).contains("\"body\" CLOB"));
        assertTrue(SqlDialect.generateCreateTableSql("t", mappings, DbType.SQLSERVER).contains("[body] NVARCHAR(MAX)"));
    }

    @Test
    void testStringIsNeverPrimaryKey() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("body", "STRING", null, 0, null));

        // 长文本不能作为主键：MySQL 拒绝无前缀长度的 TEXT/BLOB 键。
        // 界面已禁止勾选，这里验证 DDL 不会硬失败（该键被排除出 PRIMARY KEY 子句）。
        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("body"), DbType.MYSQL);
        assertTrue(sql.contains("`body` LONGTEXT NOT NULL"), sql);
        assertFalse(sql.contains("PRIMARY KEY"), "STRING 列不得进入主键子句: " + sql);
    }

    @Test
    void testStringExcludedFromCompositePrimaryKey() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));
        mappings.add(field("body", "STRING", null, 1, null));

        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("id", "body"), DbType.MYSQL);
        assertTrue(sql.contains("PRIMARY KEY (`id`)"), sql);
        assertFalse(sql.contains("`body`)"), "STRING 列不得出现在主键列表中: " + sql);
    }

    @Test
    void testMapToDbTypeDateBooleanAndTimestamp() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("birthday", "DATE", null, 1, null));
        mappings.add(field("created_at", "TIMESTAMP", null, 1, null));
        mappings.add(field("active", "BOOLEAN", null, 1, "1"));

        String sql = SqlDialect.generateCreateTableSql("config", mappings, DbType.MYSQL);
        assertTrue(sql.contains("`birthday` DATE"), sql);
        assertTrue(sql.contains("`created_at` TIMESTAMP"), sql);
        assertTrue(sql.contains("TINYINT(1)"));
        assertTrue(sql.contains("DEFAULT '1'"));
    }

    @Test
    void testPostgresDateAndBooleanUseNativeTypes() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("birthday", "DATE", null, 1, null));
        mappings.add(field("active", "BOOLEAN", null, 1, null));

        String sql = SqlDialect.generateCreateTableSql("t", mappings, DbType.POSTGRESQL);
        assertTrue(sql.contains("\"birthday\" DATE"), sql);
        assertTrue(sql.contains("\"active\" BOOLEAN"), sql);
    }

    @Test
    void testGenerateCreateTableEmptyMappings() {
        String sql = SqlDialect.generateCreateTableSql("empty_table", new ArrayList<>(), DbType.MYSQL);
        assertEquals("CREATE TABLE `empty_table` (\n)", sql);
    }

    @Test
    void testQuoteIdentifierSqlServer() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));
        String sql = SqlDialect.generateCreateTableSql("table", mappings, DbType.SQLSERVER);
        assertTrue(sql.contains("[id] BIGINT"));
    }

    private List<String> keys(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    @Test
    void testNoMergeKeysProducesNoPrimaryKey() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));
        mappings.add(field("name", "VARCHAR", 100, 1, null));

        String withEmpty = SqlDialect.generateCreateTableSql("users", mappings, keys(), DbType.MYSQL);
        String withNull = SqlDialect.generateCreateTableSql("users", mappings, null, DbType.MYSQL);
        String legacy = SqlDialect.generateCreateTableSql("users", mappings, DbType.MYSQL);

        assertFalse(withEmpty.contains("PRIMARY KEY"));
        assertFalse(withNull.contains("PRIMARY KEY"));
        assertEquals(legacy, withEmpty, "无主键时必须与旧签名产出逐字一致");
    }

    @Test
    void testSinglePrimaryKey() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));
        mappings.add(field("name", "VARCHAR", 100, 1, null));

        String sql = SqlDialect.generateCreateTableSql("users", mappings, keys("id"), DbType.MYSQL);
        assertTrue(sql.contains("PRIMARY KEY (`id`)"), sql);
        assertTrue(sql.contains("`name` VARCHAR(100),\n  PRIMARY KEY (`id`)\n)"), sql);
    }

    @Test
    void testCompositePrimaryKeyFollowsMappingOrder() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("tenant", "VARCHAR", 32, 0, null));
        mappings.add(field("id", "BIGINT", null, 0, null));

        // 传入顺序与 mappings 顺序相反，产出必须按 mappings 顺序，保证 DDL 稳定
        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("id", "tenant"), DbType.MYSQL);
        assertTrue(sql.contains("PRIMARY KEY (`tenant`, `id`)"), sql);
    }

    @Test
    void testPrimaryKeyIgnoresKeysWithoutMatchingColumn() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));

        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("id", "ghost"), DbType.MYSQL);
        assertTrue(sql.contains("PRIMARY KEY (`id`)"), sql);
        assertFalse(sql.contains("ghost"), "不得引用不存在的列: " + sql);
    }

    @Test
    void testPrimaryKeyOnlyKeyNotMappedStillIgnores() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));

        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("ghost"), DbType.MYSQL);
        assertFalse(sql.contains("PRIMARY KEY"), "全部键都无对应列时不应生成主键子句: " + sql);
    }

    @Test
    void testPrimaryKeyQuotingPerDialect() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));

        assertTrue(SqlDialect.generateCreateTableSql("t", mappings, keys("id"), DbType.MYSQL).contains("PRIMARY KEY (`id`)"));
        assertTrue(SqlDialect.generateCreateTableSql("t", mappings, keys("id"), DbType.POSTGRESQL).contains("PRIMARY KEY (\"id\")"));
        assertTrue(SqlDialect.generateCreateTableSql("t", mappings, keys("id"), DbType.SQLSERVER).contains("PRIMARY KEY ([id])"));
    }

    @Test
    void testPrimaryKeyBlankKeysIgnored() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("id", "BIGINT", null, 0, null));

        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("  ", ""), DbType.MYSQL);
        assertFalse(sql.contains("PRIMARY KEY"));
    }

    @Test
    void testPrimaryKeyColumnIsAlwaysNotNull() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        // nullable=1（可空）却同时是主键：SQL 中 PRIMARY KEY 隐含 NOT NULL，
        // 界面已强制归一，但经由接口写入或历史数据仍可能到达这里，DDL 必须兜住
        mappings.add(field("id", "BIGINT", null, 1, null));
        mappings.add(field("name", "VARCHAR", 50, 1, null));

        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("id"), DbType.MYSQL);
        assertTrue(sql.contains("`id` BIGINT NOT NULL"), sql);
        assertTrue(sql.contains("PRIMARY KEY (`id`)"), sql);
        assertFalse(sql.contains("`name` VARCHAR(50) NOT NULL"), "非主键列不应被连带改成非空: " + sql);
    }

    @Test
    void testCompositePrimaryKeyColumnsAllNotNull() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(field("tenant", "VARCHAR", 32, 1, null));
        mappings.add(field("id", "BIGINT", null, 1, null));

        String sql = SqlDialect.generateCreateTableSql("t", mappings, keys("tenant", "id"), DbType.MYSQL);
        assertTrue(sql.contains("`tenant` VARCHAR(32) NOT NULL"), sql);
        assertTrue(sql.contains("`id` BIGINT NOT NULL"), sql);
    }
}
