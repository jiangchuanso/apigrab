package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;

/**
 * 瀚高 HighGo：复用 PostgreSQL 方言（dbswitch 的 HighgoFactoryProvider 亦完全复用
 * PostgresMetadataQueryProvider / PostgresTableManageProvider）。
 */
public class HighGoDialectStrategy extends PostgreSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.HIGHGO;
    }
}
