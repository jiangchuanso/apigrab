package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL dialect: backtick quoting, multi-row VALUES and {@code ON DUPLICATE KEY UPDATE} upsert.
 */
public class MySqlDialectStrategy extends AbstractSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == null || dbType == DbType.MYSQL;
    }

    @Override
    public String getTableListQuery() {
        return "SHOW FULL TABLES";
    }

    @Override
    public String getSchemaListQuery() {
        return "SELECT schema_name FROM information_schema.schemata";
    }

    @Override
    public boolean isCatalogScope() {
        // MySQL databases are exposed as JDBC catalogs.
        return true;
    }

    @Override
    protected String quoteSimple(String identifier) {
        if (!identifier.isEmpty() && identifier.charAt(0) == '`') {
            return identifier;
        }
        return "`" + identifier + "`";
    }

    @Override
    protected String buildInsert(String quotedTable, String quotedColumns, List<String> columns, int rowCount) {
        return "INSERT INTO " + quotedTable + " (" + quotedColumns + ") VALUES " + valueRows(columns.size(), rowCount);
    }

    @Override
    protected String buildUpsert(String quotedTable, String quotedColumns, List<String> columns,
                                 List<String> keys, int rowCount) {
        return "INSERT INTO " + quotedTable + " (" + quotedColumns + ") VALUES " + valueRows(columns.size(), rowCount)
            + " ON DUPLICATE KEY UPDATE " + assignments(nonKeyColumns(columns, keys), keys);
    }

    private String assignments(List<String> nonKeys, List<String> keys) {
        if (nonKeys.isEmpty()) {
            // All columns are keys: no-op update keeps the statement valid.
            String key = quote(keys.get(0));
            return key + " = VALUES(" + key + ")";
        }
        return nonKeys.stream()
            .map(c -> {
                String q = quote(c);
                return q + " = VALUES(" + q + ")";
            })
            .collect(Collectors.joining(", "));
    }
}
