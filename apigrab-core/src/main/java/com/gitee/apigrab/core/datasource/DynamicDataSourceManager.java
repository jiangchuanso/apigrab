package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.common.util.PasswordUtils;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DynamicDataSourceManager {

    private final Map<Long, Pair<DatasourceConfigEntity, HikariDataSource>> datasourceMap = new ConcurrentHashMap<>();
    private final Map<String, URLClassLoader> classLoaderMap = new ConcurrentHashMap<>();

    @Resource
    private DatasourceConfigDao datasourceConfigDao;

    /**
     * 按需获取（惰性构建）连接：首次使用时依据库中配置创建并缓存连接池；
     * 配置变更或删除后由 {@link #destroyDataSource} 失效，下次使用重新构建。
     */
    public Connection getConnection(Long datasourceId) throws SQLException {
        return ensureDataSource(datasourceId).getConnection();
    }

    public HikariDataSource getDataSource(Long datasourceId) {
        return ensureDataSource(datasourceId);
    }

    private synchronized HikariDataSource ensureDataSource(Long datasourceId) {
        Pair<DatasourceConfigEntity, HikariDataSource> cached = datasourceMap.get(datasourceId);
        if (cached != null) {
            return cached.getRight();
        }
        DatasourceConfigEntity config = datasourceConfigDao.getById(datasourceId);
        if (config == null) {
            throw new RuntimeException("DataSource not found: " + datasourceId);
        }
        HikariDataSource ds = createHikariDataSource(config);
        datasourceMap.put(datasourceId, Pair.of(config, ds));
        log.info("Created datasource on demand: {} (id={})", config.getName(), datasourceId);
        return ds;
    }

    public synchronized void destroyDataSource(Long datasourceId) {
        Pair<DatasourceConfigEntity, HikariDataSource> pair = datasourceMap.remove(datasourceId);
        if (pair != null) {
            pair.getRight().close();
            log.info("Destroyed datasource: id={}", datasourceId);
        }
    }

    /**
     * 以给定配置做一次性连接测试（不注册连接池、不持久化）。
     * 密码：以 {@code $ENC$} 开头视为已加密并解密，否则按明文处理（用于新建态试连）。
     */
    public boolean testConnection(DatasourceConfigEntity config) {
        HikariDataSource ds = null;
        try {
            // 临时试连：密码按明文处理（新建态表单为明文）
            ds = createHikariDataSource(config, false);
            String testQuery = getTestQuery(config.getDbType());
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(testQuery);
            }
            return true;
        } catch (Exception e) {
            log.warn("Transient datasource test failed: {}", e.getMessage());
            return false;
        } finally {
            if (ds != null) {
                try { ds.close(); } catch (Exception ignore) { /* ignore */ }
            }
        }
    }

    private HikariDataSource createHikariDataSource(DatasourceConfigEntity config) {
        return createHikariDataSource(config, true);
    }

    /**
     * @param decryptPassword true=密码为库中存储的密文（解密）；false=密码为明文（临时试连）
     */
    private HikariDataSource createHikariDataSource(DatasourceConfigEntity config, boolean decryptPassword) {
        HikariConfig hikariConfig = new HikariConfig();
        String poolName = config.getId() != null
            ? "Collector-DS-" + config.getId()
            : "Collector-DS-test-" + System.nanoTime();
        hikariConfig.setPoolName(poolName);
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(decryptPassword ? PasswordUtils.decrypt(config.getPassword()) : config.getPassword());
        hikariConfig.setMaximumPoolSize(java.util.Optional.ofNullable(config.getPoolMaxSize()).orElse(10));
        hikariConfig.setMinimumIdle(java.util.Optional.ofNullable(config.getPoolMinIdle()).orElse(5));
        hikariConfig.setConnectionTimeout(30000L);
        hikariConfig.setIdleTimeout(600000L);
        hikariConfig.setMaxLifetime(1800000L);
        hikariConfig.setConnectionTestQuery(getTestQuery(config.getDbType()));

        if (StringUtils.isNotBlank(config.getDriverPath())) {
            // 使用外部 JDBC 驱动热加载
            URLClassLoader classLoader = getOrCreateClassLoader(config.getDriverPath());
            try {
                Driver driver = (Driver) classLoader.loadClass(config.getDriverClass()).newInstance();
                hikariConfig.setDriverClassName(config.getDriverClass());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("JDBC driver class not found: " + config.getDriverClass()
                    + " from path: " + config.getDriverPath() + ". Please check the JAR file exists.", e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load JDBC driver from: " + config.getDriverPath(), e);
            }
            log.info("Loaded JDBC driver from external path: {} (class={})", config.getDriverPath(), config.getDriverClass());
        } else {
            try {
                hikariConfig.setDriverClassName(config.getDriverClass());
            } catch (RuntimeException e) {
                throw new RuntimeException("JDBC driver not found: " + config.getDriverClass()
                    + ". Please either add the driver dependency or specify driverPath for hot-loading.", e);
            }
        }

        return new HikariDataSource(hikariConfig);
    }

    /**
     * 获取或创建指定路径的 URLClassLoader，支持外部 JDBC 驱动热加载
     */
    private URLClassLoader getOrCreateClassLoader(String driverPath) {
        return classLoaderMap.computeIfAbsent(driverPath, path -> {
            try {
                String[] jarPaths = path.split(",");
                URL[] urls = new URL[jarPaths.length];
                for (int i = 0; i < jarPaths.length; i++) {
                    File jarFile = new File(jarPaths[i].trim());
                    urls[i] = jarFile.toURI().toURL();
                    if (!jarFile.exists()) {
                        log.warn("JDBC driver JAR not found: {}", jarPaths[i]);
                    }
                }
                URLClassLoader loader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
                log.info("Created URLClassLoader for driver path: {}", path);
                return loader;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ClassLoader for driver path: " + path, e);
            }
        });
    }

    private String getTestQuery(DbType dbType) {
        if (dbType == DbType.ORACLE) {
            return "SELECT 1 FROM DUAL";
        }
        return "SELECT 1";
    }
}
