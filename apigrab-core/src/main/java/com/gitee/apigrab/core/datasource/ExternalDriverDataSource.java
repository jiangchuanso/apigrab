package com.gitee.apigrab.core.datasource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 用隔离的 {@link ClassLoader} 实例化厂商私有 JDBC 驱动的 DataSource。
 *
 * <p>参照 sqlrest 的 SimpleDataSource / dbswitch 的 InvisibleDataSource：直接
 * {@code Class.forName(driverClass, true, classLoader)} 后调用
 * {@link Driver#connect(String, Properties)}，绕开 {@code DriverManager} 的全局注册，
 * 避免不同版本驱动互相冲突，也避免 Hikari 的 setDriverClassName 在父加载器里
 * 找不到外部 jar 中的驱动类。
 */
public class ExternalDriverDataSource implements DataSource {

    private final Driver driver;
    private final String jdbcUrl;
    private final Properties baseProperties;
    private volatile PrintWriter logWriter;
    private volatile int loginTimeout;

    public ExternalDriverDataSource(ClassLoader loader, String driverClassName,
                                    String jdbcUrl, Properties baseProperties) {
        if (driverClassName == null || driverClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("外部 JDBC 驱动类不能为空");
        }
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL 不能为空");
        }
        try {
            Class<?> clazz = Class.forName(driverClassName.trim(), true, loader);
            this.driver = (Driver) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("外部驱动类未找到: " + driverClassName, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("外部驱动实例化失败: " + driverClassName, e);
        }
        this.jdbcUrl = jdbcUrl;
        this.baseProperties = new Properties();
        if (baseProperties != null) {
            this.baseProperties.putAll(baseProperties);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connect(null, null);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return connect(username, password);
    }

    private Connection connect(String username, String password) throws SQLException {
        Properties props = new Properties();
        props.putAll(baseProperties);
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        Connection connection = driver.connect(jdbcUrl, props);
        if (connection == null) {
            throw new SQLException("驱动 " + driver.getClass().getName() + " 不接受该 JDBC URL: " + jdbcUrl);
        }
        return connection;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public int getLoginTimeout() {
        return loginTimeout;
    }

    @Override
    public void setLoginTimeout(int seconds) {
        this.loginTimeout = seconds;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(ExternalDriverDataSource.class.getName());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
