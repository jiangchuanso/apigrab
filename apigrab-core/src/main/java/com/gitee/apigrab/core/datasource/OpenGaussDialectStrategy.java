package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;

/**
 * openGauss：PostgreSQL 兼容方言族，额外过滤 openGauss 特有的系统 schema
 * （名单对齐 dbswitch 的 OpenGaussMetadataQueryProvider）。
 */
public class OpenGaussDialectStrategy extends PostgreSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.OPENGAUSS;
    }

    @Override
    public String getSchemaListQuery() {
        return "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN "
            + "('pg_catalog', 'information_schema', 'blockchain', 'cstore', 'db4ai', "
            + "'dbe_perf', 'dbe_pldebugger', 'snapshot', 'pkgapi', 'plsql_show_all_error')";
    }
}
