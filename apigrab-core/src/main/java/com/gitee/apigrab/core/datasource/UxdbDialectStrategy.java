package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;

/**
 * 优炫 UXDB：PostgreSQL 兼容方言族（dbswitch 的 UxdbFactoryProvider 复用
 * PostgresMetadataQueryProvider / PostgresTableManageProvider）。
 */
public class UxdbDialectStrategy extends PostgreSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.UXDB;
    }
}
