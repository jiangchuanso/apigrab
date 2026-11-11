package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;

/**
 * OceanBase（MySQL 兼容模式租户）：复用 MySQL 方言。
 *
 * <p>sqlrest / dbswitch 会在连接建立后探测 ob_compatibility_mode 再切换方言；
 * apigrab 的方言在 SQL 生成期按类型静态选择，故此处固定按 MySQL 模式处理。
 * Oracle 兼容模式租户请在数据源中选择 ORACLE 类型并使用 OceanBase 驱动。
 */
public class OceanbaseDialectStrategy extends MySqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.OCEANBASE;
    }
}
