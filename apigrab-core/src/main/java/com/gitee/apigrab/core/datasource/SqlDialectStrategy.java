package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;

import java.util.List;

/**
 * Strategy for generating database-specific SQL (write statements and DDL).
 */
public interface SqlDialectStrategy {

    /** Whether this strategy handles the given database type. */
    boolean supports(DbType dbType);

    /** Quote a (possibly schema-qualified) identifier. */
    String quote(String identifier);

    /** SQL for truncating a table. */
    String truncate(String table);

    /**
     * Build a CREATE TABLE statement for the given field mappings, marking the columns named in
     * {@code mergeKeys} as the table's PRIMARY KEY.
     *
     * <p>{@code mergeKeys} reuses the task-level merge-key list: in this system the upsert conflict
     * key and the primary key are the same concept, so the field-level "is primary key" flag that
     * drives the upsert also drives the generated DDL. Columns are emitted in {@code mappings}
     * order; keys that match no mapping are ignored (DDL must not reference a non-existent column).
     */
    String buildCreateTableSql(String tableName, List<FieldMappingEntity> mappings, List<String> mergeKeys);

    /** Build a CREATE TABLE statement without a primary key. */
    default String buildCreateTableSql(String tableName, List<FieldMappingEntity> mappings) {
        return buildCreateTableSql(tableName, mappings, java.util.Collections.emptyList());
    }

    /** A cheap query used to validate a connection. */
    String getTestQuery();

    /** Query returning existing tables/views; the first column is the name. */
    String getTableListQuery();

    /** Query returning available namespaces (database/schema); the first column is the name. */
    String getSchemaListQuery();

    /** Whether the namespace maps to a JDBC catalog (true) rather than a JDBC schema (false). */
    boolean isCatalogScope();

    /** Build the write SQL for a single batch. */
    String buildWriteSql(WriteMode writeMode, String table, List<String> columns,
                         List<String> mergeKeys, int rowCount);
}
