package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.TargetType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL Server dialect: bracket quoting, multi-row VALUES,
 * {@code MERGE ... USING (VALUES ...)} upsert.
 */
public class SqlServerDialectStrategy extends AbstractSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.SQLSERVER;
    }

    @Override
    public String getTableListQuery() {
        return "SELECT name FROM sys.tables UNION SELECT name FROM sys.views";
    }

    @Override
    public String getSchemaListQuery() {
        return "SELECT name FROM sys.schemas";
    }

    @Override
    public String buildPreviewSql(String quotedTable, int limit) {
        return "SELECT TOP " + limit + " * FROM " + quotedTable;
    }

    @Override
    protected String quoteSimple(String identifier) {
        if (!identifier.isEmpty() && identifier.charAt(0) == '[') {
            return identifier;
        }
        return "[" + identifier + "]";
    }

    @Override
    protected String mapColumnType(FieldMappingEntity mapping) {
        // SQL Server 无 LONGTEXT；长文本用 NVARCHAR(MAX)。
        if (mapping.getTargetType() == TargetType.STRING) {
            return "NVARCHAR(MAX)";
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
        return "MERGE INTO " + quotedTable + " AS tgt USING (VALUES " + valueRows(columns.size(), rowCount)
            + ") AS src (" + quotedColumns + ") ON " + onClause(keys)
            + " WHEN MATCHED THEN UPDATE SET " + mergeAssignments(nonKeyColumns(columns, keys), keys)
            + " WHEN NOT MATCHED THEN INSERT (" + quotedColumns + ") VALUES ("
            + columns.stream().map(c -> "src." + quote(c)).collect(Collectors.joining(", ")) + ")";
    }

    private String onClause(List<String> keys) {
        return keys.stream()
            .map(k -> "tgt." + quote(k) + " = src." + quote(k))
            .collect(Collectors.joining(" AND "));
    }

    private String mergeAssignments(List<String> nonKeys, List<String> keys) {
        if (nonKeys.isEmpty()) {
            String key = quote(keys.get(0));
            return "tgt." + key + " = src." + key;
        }
        return nonKeys.stream()
            .map(c -> {
                String q = quote(c);
                return "tgt." + q + " = src." + q;
            })
            .collect(Collectors.joining(", "));
    }
}
