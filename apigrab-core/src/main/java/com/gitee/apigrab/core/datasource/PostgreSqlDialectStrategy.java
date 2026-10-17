package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.TargetType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL dialect: double-quote quoting, multi-row VALUES,
 * {@code ON CONFLICT (...) DO UPDATE/DO NOTHING} upsert.
 */
public class PostgreSqlDialectStrategy extends AbstractSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.POSTGRESQL;
    }

    @Override
    public String getTableListQuery() {
        return "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = current_schema() AND table_type IN ('BASE TABLE', 'VIEW')";
    }

    @Override
    public String getSchemaListQuery() {
        return "SELECT schema_name FROM information_schema.schemata "
            + "WHERE schema_name NOT IN ('pg_catalog', 'information_schema')";
    }

    @Override
    protected String quoteSimple(String identifier) {
        if (!identifier.isEmpty() && identifier.charAt(0) == '"') {
            return identifier;
        }
        return "\"" + identifier + "\"";
    }

    @Override
    protected String mapColumnType(FieldMappingEntity mapping) {
        TargetType type = mapping.getTargetType();
        // DATE 与 TIMESTAMP 现在分别是独立的枚举值，基础实现已直接映射为 DATE / TIMESTAMP，
        // 无需再像过去那样把 DATE 提升为 TIMESTAMP。
        // STRING 是长文本，PostgreSQL 下用 TEXT（基础实现的 LONGTEXT 是 MySQL 专用类型）。
        if (type == TargetType.STRING) {
            return "TEXT";
        }
        if (type == TargetType.BOOLEAN) {
            return "BOOLEAN";
        }
        return super.mapColumnType(mapping);
    }

    @Override
    protected String buildInsert(String quotedTable, String quotedColumns, List<String> columns, int rowCount) {
        return "INSERT INTO " + quotedTable + " (" + quotedColumns + ") VALUES " + valueRows(columns.size(), rowCount);
    }

    @Override
    protected String buildUpsert(String quotedTable, String quotedColumns, List<String> columns,
                                 List<String> keys, int rowCount) {
        String conflict = keys.stream().map(this::quote).collect(Collectors.joining(", "));
        String base = "INSERT INTO " + quotedTable + " (" + quotedColumns + ") VALUES "
            + valueRows(columns.size(), rowCount) + " ON CONFLICT (" + conflict + ") ";
        List<String> nonKeys = nonKeyColumns(columns, keys);
        if (nonKeys.isEmpty()) {
            return base + "DO NOTHING";
        }
        String set = nonKeys.stream()
            .map(c -> quote(c) + " = EXCLUDED." + quote(c))
            .collect(Collectors.joining(", "));
        return base + "DO UPDATE SET " + set;
    }
}
