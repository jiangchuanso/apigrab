package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.TargetType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Oracle dialect: double-quote quoting, {@code INSERT ALL ... SELECT 1 FROM DUAL}
 * for multi-row inserts, and {@code MERGE ... USING (SELECT ? ... FROM DUAL UNION ALL ...)}
 * upsert.
 */
public class OracleDialectStrategy extends AbstractSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.ORACLE;
    }

    @Override
    public String getTestQuery() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    public String getTableListQuery() {
        return "SELECT table_name FROM user_tables UNION SELECT view_name FROM user_views";
    }

    @Override
    public String getSchemaListQuery() {
        return "SELECT username FROM all_users";
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
        // Oracle 无 LONGTEXT；长文本用 CLOB。
        if (mapping.getTargetType() == TargetType.STRING) {
            return "CLOB";
        }
        return super.mapColumnType(mapping);
    }

    @Override
    protected String buildInsert(String quotedTable, String quotedColumns, List<String> columns, int rowCount) {
        // Oracle has no multi-row VALUES before 23c; use INSERT ALL.
        StringBuilder sb = new StringBuilder("INSERT ALL");
        for (int r = 0; r < rowCount; r++) {
            sb.append(" INTO ").append(quotedTable)
                .append(" (").append(quotedColumns).append(") VALUES ")
                .append(placeholders(columns.size()));
        }
        return sb.append(" SELECT 1 FROM DUAL").toString();
    }

    @Override
    protected String buildUpsert(String quotedTable, String quotedColumns, List<String> columns,
                                 List<String> keys, int rowCount) {
        StringBuilder sb = new StringBuilder("MERGE INTO ").append(quotedTable).append(" tgt USING (");
        for (int r = 0; r < rowCount; r++) {
            if (r > 0) {
                sb.append(" UNION ALL ");
            }
            sb.append("SELECT ");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("? AS ").append(quote(columns.get(i)));
            }
            sb.append(" FROM DUAL");
        }
        sb.append(") src ON ").append(onClause(keys))
            .append(" WHEN MATCHED THEN UPDATE SET ").append(mergeAssignments(nonKeyColumns(columns, keys), keys))
            .append(" WHEN NOT MATCHED THEN INSERT (").append(quotedColumns).append(") VALUES (")
            .append(columns.stream().map(c -> "src." + quote(c)).collect(Collectors.joining(", ")))
            .append(")");
        return sb.toString();
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
