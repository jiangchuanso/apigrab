package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.TargetType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 国产数据库方言与连接元数据回归测试。 */
class DomesticDialectTest {

    private static final List<DbType> DOMESTIC = Arrays.asList(
        DbType.DM, DbType.KINGBASE, DbType.OSCAR, DbType.GBASE8A,
        DbType.HIGHGO, DbType.OPENGAUSS, DbType.OCEANBASE, DbType.UXDB);

    private final List<String> columns = Arrays.asList("id", "body");
    private final List<String> keys = Collections.singletonList("id");

    private FieldMappingEntity field(String name, TargetType type) {
        FieldMappingEntity f = new FieldMappingEntity();
        f.setTargetField(name);
        f.setTargetType(type);
        return f;
    }

    @Test
    void testEachDomesticTypeResolvesItsOwnStrategy() {
        assertEquals(DmDialectStrategy.class, SqlDialect.resolve(DbType.DM).getClass());
        assertEquals(KingbaseDialectStrategy.class, SqlDialect.resolve(DbType.KINGBASE).getClass());
        assertEquals(OscarDialectStrategy.class, SqlDialect.resolve(DbType.OSCAR).getClass());
        assertEquals(Gbase8aDialectStrategy.class, SqlDialect.resolve(DbType.GBASE8A).getClass());
        assertEquals(HighGoDialectStrategy.class, SqlDialect.resolve(DbType.HIGHGO).getClass());
        assertEquals(OpenGaussDialectStrategy.class, SqlDialect.resolve(DbType.OPENGAUSS).getClass());
        assertEquals(OceanbaseDialectStrategy.class, SqlDialect.resolve(DbType.OCEANBASE).getClass());
        assertEquals(UxdbDialectStrategy.class, SqlDialect.resolve(DbType.UXDB).getClass());
    }

    @Test
    void testEveryDomesticTypeHasConnectionMetadata() {
        for (DbType type : DOMESTIC) {
            assertTrue(type.isExternalDriver(), type + " 应使用外部驱动目录");
            assertNotNull(type.getDriverClass());
            assertTrue(type.getUrlPrefix().startsWith("jdbc:"));
            assertTrue(type.getUrlSample().startsWith(type.getUrlPrefix()));
            assertTrue(type.getDefaultPort() > 0);
            assertEquals(type, DbType.fromName(type.getDriverDirectory()));
            assertEquals(type, DbType.fromName(type.name().toLowerCase()));
        }
        assertEquals("gbase8a", DbType.GBASE8A.getDriverDirectory());
        assertEquals("opengauss", DbType.OPENGAUSS.getDriverDirectory());
        assertEquals("com.kingbase8.Driver", DbType.KINGBASE.getDriverClass());
        assertNull(DbType.fromName("unknown-db"));
    }

    @Test
    void testQuotingPerDialectFamily() {
        assertEquals("\"schema\".\"t\"", SqlDialect.quote("schema.t", DbType.DM));
        assertEquals("\"t\"", SqlDialect.quote("t", DbType.KINGBASE));
        assertEquals("`schema`.`t`", SqlDialect.quote("schema.t", DbType.GBASE8A));
        assertEquals("`t`", SqlDialect.quote("t", DbType.OCEANBASE));
    }

    @Test
    void testTestQueryAndPreviewSql() {
        assertEquals("SELECT 1 FROM DUAL", SqlDialect.resolve(DbType.DM).getTestQuery());
        assertEquals("SELECT * FROM \"t\" WHERE ROWNUM <= 10",
            SqlDialect.resolve(DbType.DM).buildPreviewSql("\"t\"", 10));
        for (DbType type : DOMESTIC) {
            if (type != DbType.DM) {
                assertEquals("SELECT 1", SqlDialect.resolve(type).getTestQuery());
            }
        }
        assertEquals("SELECT * FROM `t` LIMIT 20",
            SqlDialect.resolve(DbType.GBASE8A).buildPreviewSql("`t`", 20));
        assertEquals("SELECT * FROM \"t\" LIMIT 20",
            SqlDialect.resolve(DbType.OPENGAUSS).buildPreviewSql("\"t\"", 20));
    }

    @Test
    void testExistingDialectPreviewSql() {
        assertEquals("SELECT * FROM `t` LIMIT 5",
            SqlDialect.resolve(DbType.MYSQL).buildPreviewSql("`t`", 5));
        assertEquals("SELECT * FROM \"t\" WHERE ROWNUM <= 5",
            SqlDialect.resolve(DbType.ORACLE).buildPreviewSql("\"t\"", 5));
        assertEquals("SELECT TOP 5 * FROM [t]",
            SqlDialect.resolve(DbType.SQLSERVER).buildPreviewSql("[t]", 5));
    }

    @Test
    void testColumnTypeMapping() {
        String dm = SqlDialect.generateCreateTableSql("t",
            Arrays.asList(field("body", TargetType.STRING), field("active", TargetType.BOOLEAN)), DbType.DM);
        assertTrue(dm.contains("\"body\" CLOB"), dm);
        assertTrue(dm.contains("\"active\" BIT"), dm);

        String pg = SqlDialect.generateCreateTableSql("t",
            Arrays.asList(field("body", TargetType.STRING), field("active", TargetType.BOOLEAN)), DbType.OPENGAUSS);
        assertTrue(pg.contains("\"body\" TEXT"), pg);
        assertTrue(pg.contains("\"active\" BOOLEAN"), pg);

        String oscar = SqlDialect.generateCreateTableSql("t",
            Collections.singletonList(field("active", TargetType.BOOLEAN)), DbType.OSCAR);
        assertTrue(oscar.contains("\"active\" BIT"), oscar);

        String gbase = SqlDialect.generateCreateTableSql("t",
            Collections.singletonList(field("body", TargetType.STRING)), DbType.GBASE8A);
        assertTrue(gbase.contains("`body` LONGTEXT"), gbase);
    }

    @Test
    void testWriteSqlPerFamily() {
        String dmInsert = SqlDialect.buildWriteSql(DbType.DM, WriteMode.INSERT, "t", columns, null, 2);
        assertTrue(dmInsert.startsWith("INSERT ALL"), dmInsert);
        assertTrue(dmInsert.endsWith("SELECT 1 FROM DUAL"), dmInsert);

        String dmMerge = SqlDialect.buildWriteSql(DbType.DM, WriteMode.MERGE, "t", columns, keys, 1);
        assertTrue(dmMerge.startsWith("MERGE INTO \"t\" tgt USING ("), dmMerge);

        String pgInsert = SqlDialect.buildWriteSql(DbType.KINGBASE, WriteMode.INSERT, "t", columns, null, 2);
        assertEquals("INSERT INTO \"t\" (\"id\", \"body\") VALUES (?, ?), (?, ?)", pgInsert);

        String pgMerge = SqlDialect.buildWriteSql(DbType.OPENGAUSS, WriteMode.MERGE, "t", columns, keys, 1);
        assertTrue(pgMerge.contains("ON CONFLICT (\"id\") DO UPDATE SET"), pgMerge);

        String gbaseMerge = SqlDialect.buildWriteSql(DbType.GBASE8A, WriteMode.MERGE, "t", columns, keys, 1);
        assertTrue(gbaseMerge.contains("ON DUPLICATE KEY UPDATE"), gbaseMerge);
    }

    @Test
    void testMetadataQueries() {
        assertTrue(SqlDialect.resolve(DbType.DM).getSchemaListQuery().contains("ALL_OBJECTS"));
        assertTrue(SqlDialect.resolve(DbType.DM).getTableListQuery().contains("user_tables"));
        assertTrue(SqlDialect.resolve(DbType.GBASE8A).getTableListQuery().contains("information_schema.tables"));
        assertTrue(SqlDialect.resolve(DbType.OPENGAUSS).getSchemaListQuery().contains("dbe_perf"));
        assertTrue(SqlDialect.resolve(DbType.KINGBASE).getSchemaListQuery().contains("information_schema.schemata"));
        assertTrue(SqlDialect.resolve(DbType.GBASE8A).isCatalogScope());
        assertTrue(SqlDialect.resolve(DbType.OCEANBASE).isCatalogScope());
        assertFalse(SqlDialect.resolve(DbType.DM).isCatalogScope());
        assertFalse(SqlDialect.resolve(DbType.UXDB).isCatalogScope());
    }

    @Test
    void testTruncateSqlPerFamily() {
        assertEquals("TRUNCATE TABLE \"t\"", SqlDialect.truncateSql("t", DbType.DM));
        assertEquals("TRUNCATE TABLE `t`", SqlDialect.truncateSql("t", DbType.GBASE8A));
    }
}
