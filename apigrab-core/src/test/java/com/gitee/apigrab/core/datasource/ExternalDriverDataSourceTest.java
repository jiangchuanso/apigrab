package com.gitee.apigrab.core.datasource;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** 外部驱动通过隔离 ClassLoader 实例化并由 Hikari 以 DataSource 方式使用。 */
class ExternalDriverDataSourceTest {

    /** 假驱动：命中 URL 时抛出标记异常，以便断言 connect 确实被调用且参数正确。 */
    public static class FakeDriver implements Driver {
        static volatile String lastUrl;
        static volatile Properties lastProps;

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            lastUrl = url;
            lastProps = info;
            if (url != null && url.startsWith("jdbc:fake:")) {
                throw new SQLException("FAKE_CONNECTED");
            }
            return null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:fake:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return Logger.getLogger(FakeDriver.class.getName());
        }
    }

    @Test
    void testConnectPassesUrlCredentialsAndConnProps() {
        Properties base = new Properties();
        base.setProperty("charset", "utf8");
        ExternalDriverDataSource ds = new ExternalDriverDataSource(
            getClass().getClassLoader(), FakeDriver.class.getName(), "jdbc:fake://localhost/db", base);

        SQLException ex = assertThrows(SQLException.class, () -> ds.getConnection("scott", "tiger"));
        assertEquals("FAKE_CONNECTED", ex.getMessage());
        assertEquals("jdbc:fake://localhost/db", FakeDriver.lastUrl);
        assertEquals("scott", FakeDriver.lastProps.getProperty("user"));
        assertEquals("tiger", FakeDriver.lastProps.getProperty("password"));
        assertEquals("utf8", FakeDriver.lastProps.getProperty("charset"));
        // 传入的 Properties 不被 connect 污染
        assertNull(base.getProperty("user"));
    }

    @Test
    void testRejectsUrlNotAcceptedByDriver() {
        ExternalDriverDataSource ds = new ExternalDriverDataSource(
            getClass().getClassLoader(), FakeDriver.class.getName(), "jdbc:other://host/db", null);
        SQLException ex = assertThrows(SQLException.class, ds::getConnection);
        assertTrue(ex.getMessage().contains("不接受该 JDBC URL"));
    }

    @Test
    void testMissingDriverClassFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalDriverDataSource(
            getClass().getClassLoader(), "com.example.NoSuchDriver", "jdbc:fake://host/db", null));
        assertThrows(IllegalArgumentException.class, () -> new ExternalDriverDataSource(
            getClass().getClassLoader(), FakeDriver.class.getName(), "", null));
    }

    @Test
    void testDataSourceWrapperContract() throws Exception {
        ExternalDriverDataSource ds = new ExternalDriverDataSource(
            getClass().getClassLoader(), FakeDriver.class.getName(), "jdbc:fake://host/db", null);
        assertTrue(ds.isWrapperFor(ExternalDriverDataSource.class));
        assertSame(ds, ds.unwrap(ExternalDriverDataSource.class));
        assertNotNull(ds.getParentLogger());
        ds.setLoginTimeout(5);
        assertEquals(5, ds.getLoginTimeout());
    }
}
