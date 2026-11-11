package com.gitee.apigrab.core.datasource;

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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
            String testQuery = SqlDialect.resolve(config.getDbType()).getTestQuery();
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
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(decryptPassword ? PasswordUtils.decrypt(config.getPassword()) : config.getPassword());
        hikariConfig.setMaximumPoolSize(java.util.Optional.ofNullable(config.getPoolMaxSize()).orElse(10));
        hikariConfig.setMinimumIdle(java.util.Optional.ofNullable(config.getPoolMinIdle()).orElse(5));
        hikariConfig.setConnectionTimeout(30000L);
        hikariConfig.setIdleTimeout(600000L);
        hikariConfig.setMaxLifetime(1800000L);
        // 探活 SQL 统一取方言，国产库中达梦等 Oracle 兼容库需要 FROM DUAL。
        hikariConfig.setConnectionTestQuery(SqlDialect.resolve(config.getDbType()).getTestQuery());

        String driverClass = resolveDriverClass(config);
        if (StringUtils.isNotBlank(config.getDriverPath())) {
            // 厂商私有驱动：隔离 ClassLoader 实例化 Driver，交给 Hikari 的 DataSource，
            // 不走 setDriverClassName（父加载器看不到外部 jar 中的类）。
            try {
                URLClassLoader classLoader = getOrCreateClassLoader(config.getDriverPath());
                hikariConfig.setDataSource(new ExternalDriverDataSource(
                    classLoader, driverClass, config.getJdbcUrl(), toProperties(config.getConnProps())));
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to load JDBC driver from: " + config.getDriverPath(), e);
            }
            log.info("Loaded JDBC driver from external path: {} (class={})", config.getDriverPath(), driverClass);
        } else {
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            try {
                hikariConfig.setDriverClassName(driverClass);
            } catch (RuntimeException e) {
                throw new RuntimeException("JDBC driver not found: " + driverClass
                    + ". Please either add the driver dependency or specify driverPath for hot-loading.", e);
            }
        }

        return new HikariDataSource(hikariConfig);
    }

    /** 表单未显式指定驱动类时，用 DbType 自带的默认驱动类兜底。 */
    private String resolveDriverClass(DatasourceConfigEntity config) {
        if (StringUtils.isNotBlank(config.getDriverClass())) {
            return config.getDriverClass().trim();
        }
        if (config.getDbType() != null) {
            return config.getDbType().getDriverClass();
        }
        throw new IllegalArgumentException("未指定 JDBC 驱动类，且无法由数据库类型推断");
    }

    private Properties toProperties(Map<String, Object> connProps) {
        Properties properties = new Properties();
        if (connProps != null) {
            for (Map.Entry<String, Object> entry : connProps.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    properties.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }
        return properties;
    }

    /**
     * 获取或创建指定路径的 URLClassLoader，支持外部 JDBC 驱动热加载。
     * 路径项可以是单个 jar，也可以是包含 jar 的目录（如 {@code drivers/dm/dm-8}）。
     */
    private URLClassLoader getOrCreateClassLoader(String driverPath) {
        return classLoaderMap.computeIfAbsent(driverPath, path -> {
            try {
                List<URL> urls = new ArrayList<>();
                for (String item : path.split(",")) {
                    String trimmed = item.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    File file = new File(trimmed);
                    if (!file.exists()) {
                        log.warn("JDBC driver path not found: {}", trimmed);
                        continue;
                    }
                    if (file.isDirectory()) {
                        collectJars(file, urls);
                    } else {
                        urls.add(file.toURI().toURL());
                    }
                }
                if (urls.isEmpty()) {
                    throw new IllegalArgumentException("外部驱动路径下未找到任何 jar: " + path);
                }
                URLClassLoader loader = new URLClassLoader(
                    urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
                log.info("Created URLClassLoader for driver path: {} ({} jar)", path, urls.size());
                return loader;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ClassLoader for driver path: " + path, e);
            }
        });
    }

    private void collectJars(File directory, List<URL> urls) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            if (file.isDirectory()) {
                collectJars(file, urls);
            } else if (file.getName().toLowerCase().endsWith(".jar")) {
                urls.add(file.toURI().toURL());
            }
        }
    }
}
