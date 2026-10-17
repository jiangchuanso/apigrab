package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;

import java.util.Arrays;
import java.util.List;

/**
 * Facade/context for database-dialect-aware SQL. Selects a {@link SqlDialectStrategy}
 * by database type (MySQL is the default when the type is null).
 */
public final class SqlDialect {

    private static final SqlDialectStrategy MYSQL = new MySqlDialectStrategy();

    private static final List<SqlDialectStrategy> STRATEGIES = Arrays.asList(
        MYSQL,
        new PostgreSqlDialectStrategy(),
        new SqlServerDialectStrategy(),
        new OracleDialectStrategy()
    );

    private SqlDialect() {
    }

    /** Resolve the strategy for the given database type, falling back to MySQL. */
    public static SqlDialectStrategy resolve(DbType dbType) {
        for (SqlDialectStrategy strategy : STRATEGIES) {
            if (strategy.supports(dbType)) {
                return strategy;
            }
        }
        return MYSQL;
    }

    public static String quote(String identifier, DbType dbType) {
        return resolve(dbType).quote(identifier);
    }

    public static String truncateSql(String table, DbType dbType) {
        return resolve(dbType).truncate(table);
    }

    public static String buildWriteSql(DbType dbType, WriteMode writeMode, String table,
                                       List<String> columns, List<String> mergeKeys, int rowCount) {
        return resolve(dbType).buildWriteSql(writeMode, table, columns, mergeKeys, rowCount);
    }

    /** Build a CREATE TABLE statement using the dialect selected for {@code dbType}, without a primary key. */
    public static String generateCreateTableSql(String tableName, List<FieldMappingEntity> mappings, DbType dbType) {
        return resolve(dbType).buildCreateTableSql(tableName, mappings);
    }

    /**
     * Build a CREATE TABLE statement using the dialect selected for {@code dbType}, marking the
     * columns named in {@code mergeKeys} as the PRIMARY KEY.
     *
     * @param mergeKeys task-level merge keys; in this system they double as the primary key
     */
    public static String generateCreateTableSql(String tableName, List<FieldMappingEntity> mappings,
                                                List<String> mergeKeys, DbType dbType) {
        return resolve(dbType).buildCreateTableSql(tableName, mappings, mergeKeys);
    }
}
