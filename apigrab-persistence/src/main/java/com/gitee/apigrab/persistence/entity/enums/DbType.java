package com.gitee.apigrab.persistence.entity.enums;

import java.util.Locale;

/**
 * 目标数据库类型。
 *
 * <p>连接元数据的组织方式对齐同作者的 sqlrest / dbswitch：每个类型自带驱动类、
 * URL 前缀、默认端口与示例 URL，避免前后端多处硬编码清单。
 *
 * <p>国产数据库 JDBC 驱动多为厂商私有 jar，不进入 Maven classpath，而是由
 * {@code drivers/<type>/<version>/*.jar} 外部目录在运行期动态加载；
 * {@link #isExternalDriver()} 用于提示界面必须指定驱动路径。
 */
public enum DbType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://", 3306,
        "jdbc:mysql://localhost:3306/your_database?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai", false),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@", 1521,
        "jdbc:oracle:thin:@localhost:1521:your_sid", false),
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://", 1433,
        "jdbc:sqlserver://localhost:1433;databaseName=your_database", false),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://", 5432,
        "jdbc:postgresql://localhost:5432/your_database", false),

    // 国产数据库：驱动 jar 放在 drivers/<类型名小写>/<版本>/ 下，运行时 URLClassLoader 加载
    DM("达梦 DM", "dm.jdbc.driver.DmDriver", "jdbc:dm://", 5236,
        "jdbc:dm://localhost:5236/your_database", true),
    KINGBASE("人大金仓 KingbaseES", "com.kingbase8.Driver", "jdbc:kingbase8://", 54321,
        "jdbc:kingbase8://localhost:54321/your_database", true),
    OSCAR("神通 Oscar", "com.oscar.Driver", "jdbc:oscar://", 2003,
        "jdbc:oscar://localhost:2003/OSCRDB", true),
    GBASE8A("南大通用 GBase8A", "com.gbase.jdbc.Driver", "jdbc:gbase://", 5258,
        "jdbc:gbase://localhost:5258/your_database", true),
    HIGHGO("瀚高 HighGo", "com.highgo.jdbc.Driver", "jdbc:highgo://", 5866,
        "jdbc:highgo://localhost:5866/your_database", true),
    OPENGAUSS("openGauss", "org.opengauss.Driver", "jdbc:opengauss://", 15432,
        "jdbc:opengauss://localhost:15432/your_database", true),
    // OceanBase 有 MySQL / Oracle 两种租户兼容模式；此处按 MySQL 模式方言处理，
    // Oracle 模式租户可选用 ORACLE 类型配合 OceanBase 驱动。
    OCEANBASE("OceanBase（MySQL 模式）", "com.oceanbase.jdbc.Driver", "jdbc:oceanbase://", 2881,
        "jdbc:oceanbase://localhost:2881/your_database", true),
    UXDB("优炫 UXDB", "com.uxsino.uxdb.Driver", "jdbc:uxdb://", 52025,
        "jdbc:uxdb://localhost:52025/your_database", true);

    private final String displayName;
    private final String driverClass;
    private final String urlPrefix;
    private final int defaultPort;
    private final String urlSample;
    private final boolean externalDriver;

    DbType(String displayName, String driverClass, String urlPrefix, int defaultPort,
           String urlSample, boolean externalDriver) {
        this.displayName = displayName;
        this.driverClass = driverClass;
        this.urlPrefix = urlPrefix;
        this.defaultPort = defaultPort;
        this.urlSample = urlSample;
        this.externalDriver = externalDriver;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getUrlSample() {
        return urlSample;
    }

    public boolean isExternalDriver() {
        return externalDriver;
    }

    /** 与 sqlrest / dbswitch 的 {@code drivers/<name>/<version>} 目录名对齐。 */
    public String getDriverDirectory() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 按枚举名或驱动目录名解析，大小写不敏感；未匹配返回 {@code null}。 */
    public static DbType fromName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (DbType type : values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(normalized)
                || type.getDriverDirectory().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
