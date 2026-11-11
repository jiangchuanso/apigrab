package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 外部驱动目录扫描（对齐 sqlrest / dbswitch 的 drivers/<type>/<version> 布局）。 */
class DriverCatalogTest {

    @TempDir
    Path root;

    @Test
    void testScanVersionedPackages() throws Exception {
        Path dmDir = Files.createDirectories(root.resolve("dm/dm-8"));
        Files.write(dmDir.resolve("DmJdbcDriver18.jar"), new byte[]{1});
        Path kingbaseDir = Files.createDirectories(root.resolve("kingbase/kingbase-v8r6"));
        Files.write(kingbaseDir.resolve("kingbase8-8.6.0.jar"), new byte[]{1});
        // 未登记的类型目录必须被忽略
        Path unknown = Files.createDirectories(root.resolve("not-a-db/v1"));
        Files.write(unknown.resolve("x.jar"), new byte[]{1});

        List<Map<String, Object>> packages = DriverCatalog.scan(Collections.singletonList(root.toFile()));

        assertEquals(2, packages.size());
        Map<String, Object> dm = packages.get(0);
        assertEquals("DM", dm.get("dbType"));
        assertEquals("dm-8", dm.get("version"));
        assertEquals("dm.jdbc.driver.DmDriver", dm.get("driverClass"));
        assertTrue(((String) dm.get("driverPath")).endsWith("DmJdbcDriver18.jar"));
        assertEquals(Collections.singletonList("DmJdbcDriver18.jar"), dm.get("jarFiles"));

        Map<String, Object> kingbase = packages.get(1);
        assertEquals("KINGBASE", kingbase.get("dbType"));
        assertEquals("kingbase-v8r6", kingbase.get("version"));
    }

    @Test
    void testScanJarsDirectlyUnderTypeDirectory() throws Exception {
        Path dir = Files.createDirectories(root.resolve("opengauss"));
        Files.write(dir.resolve("opengauss-jdbc-5.0.1.jar"), new byte[]{1});

        List<Map<String, Object>> packages = DriverCatalog.scan(Collections.singletonList(root.toFile()));

        assertEquals(1, packages.size());
        assertEquals("default", packages.get(0).get("version"));
        assertEquals("OPENGAUSS", packages.get(0).get("dbType"));
    }

    @Test
    void testScanIgnoresMissingRootAndEmptyTypeDir() throws Exception {
        Files.createDirectories(root.resolve("uxdb"));
        assertTrue(DriverCatalog.scan(Collections.singletonList(root.toFile())).isEmpty());
        assertTrue(DriverCatalog.scan(Collections.singletonList(root.resolve("missing").toFile())).isEmpty());
        assertTrue(DriverCatalog.scan(null).isEmpty());
    }

    @Test
    void testDriverDirectoryNamesMatchSiblingProjects() {
        // 目录名必须与 dbswitch / sqlrest 的 drivers/ 下目录一致，便于直接复用驱动包
        assertEquals("dm", DbType.DM.getDriverDirectory());
        assertEquals("kingbase", DbType.KINGBASE.getDriverDirectory());
        assertEquals("oscar", DbType.OSCAR.getDriverDirectory());
        assertEquals("gbase8a", DbType.GBASE8A.getDriverDirectory());
        assertEquals("highgo", DbType.HIGHGO.getDriverDirectory());
        assertEquals("opengauss", DbType.OPENGAUSS.getDriverDirectory());
        assertEquals("oceanbase", DbType.OCEANBASE.getDriverDirectory());
        assertEquals("uxdb", DbType.UXDB.getDriverDirectory());
    }

    @Test
    void testConfiguredRootsWithoutSpringInjection() {
        assertTrue(new DriverCatalog().configuredRoots().isEmpty());
        assertDoesNotThrow(() -> new DriverCatalog().refresh());
    }
}
