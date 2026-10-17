package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.TargetType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared template for {@link SqlDialectStrategy} implementations.
 * Handles write-mode dispatch and common SQL fragment building; subclasses
 * supply dialect-specific quoting and insert/upsert syntax.
 */
public abstract class AbstractSqlDialectStrategy implements SqlDialectStrategy {

    /** Quote a single identifier using this dialect's quoting rules. */
    protected abstract String quoteSimple(String identifier);

    /** Build a plain multi-row (or dialect-equivalent) insert statement. */
    protected abstract String buildInsert(String quotedTable, String quotedColumns, List<String> columns, int rowCount);

    /**
     * Build an upsert (merge) statement.
     *
     * @param columns unquoted column names
     * @param keys    normalized (non-empty) merge key names
     */
    protected abstract String buildUpsert(String quotedTable, String quotedColumns,
                                          List<String> columns, List<String> keys, int rowCount);

    @Override
    public String quote(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.contains(".")) {
            String[] parts = trimmed.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append('.');
                }
                sb.append(quoteSimple(parts[i]));
            }
            return sb.toString();
        }
        return quoteSimple(trimmed);
    }

    @Override
    public String truncate(String table) {
        return "TRUNCATE TABLE " + quote(table);
    }

    @Override
    public String getTestQuery() {
        return "SELECT 1";
    }

    @Override
    public boolean isCatalogScope() {
        return false;
    }

    @Override
    public String buildCreateTableSql(String tableName, List<FieldMappingEntity> mappings, List<String> mergeKeys) {
        List<String> primaryKey = resolvePrimaryKeyColumns(mappings, mergeKeys);
        Set<String> primaryKeySet = new HashSet<>(primaryKey);

        List<String> definitions = new ArrayList<>();
        for (FieldMappingEntity mapping : mappings) {
            StringBuilder column = new StringBuilder();
            column.append(quote(mapping.getTargetField())).append(" ").append(mapColumnType(mapping));
            // 主键列必须非空：SQL 中 PRIMARY KEY 隐含 NOT NULL。这里独立于 nullable 判断，
            // 是为了让经由接口写入、或历史上存成「主键 + 可空」的配置也无法建出矛盾的表。
            boolean notNull = (mapping.getNullable() != null && mapping.getNullable() == 0)
                || isPrimaryKeyColumn(mapping, primaryKeySet);
            if (notNull) {
                column.append(" NOT NULL");
            }
            if (mapping.getDefaultValue() != null && !mapping.getDefaultValue().isEmpty()) {
                column.append(" DEFAULT '").append(mapping.getDefaultValue()).append("'");
            }
            definitions.add(column.toString());
        }

        if (!primaryKey.isEmpty()) {
            StringBuilder pk = new StringBuilder("PRIMARY KEY (");
            for (int i = 0; i < primaryKey.size(); i++) {
                if (i > 0) {
                    pk.append(", ");
                }
                pk.append(quote(primaryKey.get(i)));
            }
            pk.append(")");
            definitions.add(pk.toString());
        }

        StringBuilder sb = new StringBuilder("CREATE TABLE ").append(quote(tableName)).append(" (\n");
        for (int i = 0; i < definitions.size(); i++) {
            sb.append("  ").append(definitions.get(i));
            if (i < definitions.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    private boolean isPrimaryKeyColumn(FieldMappingEntity mapping, Set<String> primaryKeySet) {
        String field = mapping.getTargetField();
        return field != null && primaryKeySet.contains(field.trim());
    }

    /**
     * Intersect the merge-key list with the mapped target columns, preserving {@code mappings} order
     * so the same configuration always yields the same DDL.
     *
     * <p>Keys that match no mapping are dropped rather than emitted: the DDL must never reference a
     * column that does not exist in the statement. (The UI surfaces unmatched keys to the user.)
     */
    private List<String> resolvePrimaryKeyColumns(List<FieldMappingEntity> mappings, List<String> mergeKeys) {
        if (mergeKeys == null || mergeKeys.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> wanted = new HashSet<>();
        for (String key : mergeKeys) {
            if (key != null && !key.trim().isEmpty()) {
                wanted.add(key.trim());
            }
        }
        if (wanted.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ordered = new ArrayList<>();
        for (FieldMappingEntity mapping : mappings) {
            String field = mapping.getTargetField();
            if (field != null && wanted.contains(field.trim()) && isPrimaryKeyCapable(mapping)) {
                ordered.add(field.trim());
            }
        }
        return ordered;
    }

    /**
     * 该字段是否可作为主键。长文本类型（{@code STRING}）**不能**作为主键：
     * MySQL 直接拒绝无前缀长度的 TEXT/BLOB 键，其它方言亦无等价的可用语义。
     *
     * <p>界面已禁止为 STRING 勾选主键，并在加载/保存时归一；此处再排除一次，
     * 是为了避免经由接口写入的配置在建表时硬失败——被排除的键不会出现在 PRIMARY KEY 子句中。
     */
    private boolean isPrimaryKeyCapable(FieldMappingEntity mapping) {
        return mapping.getTargetType() != TargetType.STRING;
    }

    /**
     * Map a field mapping to this dialect's column type. Subclasses may override
     * for dialect-specific types.
     */
    protected String mapColumnType(FieldMappingEntity mapping) {
        TargetType type = Optional.ofNullable(mapping.getTargetType()).orElse(TargetType.VARCHAR);
        Integer length = mapping.getTargetLength();
        switch (type) {
            case VARCHAR:
                return "VARCHAR(" + Optional.ofNullable(length).orElse(255) + ")";
            // STRING 是**长文本**，不是 VARCHAR 的别名：MySQL 下用 LONGTEXT。
            // 其它方言各自覆盖（PostgreSQL→TEXT、Oracle→CLOB、SQL Server→NVARCHAR(MAX)）。
            case STRING:
                return "LONGTEXT";
            case BIGINT:
                return "BIGINT";
            case DECIMAL:
                return "DECIMAL(18,6)";
            case DATE:
                return "DATE";
            case TIMESTAMP:
                return "TIMESTAMP";
            case BOOLEAN:
                return "TINYINT(1)";
            default:
                return "VARCHAR(255)";
        }
    }

    @Override
    public String buildWriteSql(WriteMode writeMode, String table, List<String> columns,
                                List<String> mergeKeys, int rowCount) {
        String quotedTable = quote(table);
        String quotedColumns = quoteColumns(columns);
        WriteMode mode = Optional.ofNullable(writeMode).orElse(WriteMode.INSERT);

        if (mode == WriteMode.MERGE) {
            if (isBlank(mergeKeys)) {
                throw new IllegalArgumentException("MERGE 模式需要配置合并主键 mergeKeys");
            }
            return buildUpsert(quotedTable, quotedColumns, columns, normalizeKeys(mergeKeys), rowCount);
        }
        // INSERT / TRUNCATE_INSERT / default
        return buildInsert(quotedTable, quotedColumns, columns, rowCount);
    }

    protected String quoteColumns(List<String> columns) {
        return columns.stream().map(this::quote).collect(Collectors.joining(", "));
    }

    protected static String placeholders(int columnCount) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        return sb.append(")").toString();
    }

    protected static String valueRows(int columnCount, int rowCount) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rowCount; r++) {
            if (r > 0) {
                sb.append(", ");
            }
            sb.append(placeholders(columnCount));
        }
        return sb.toString();
    }

    protected static List<String> normalizeKeys(List<String> mergeKeys) {
        return mergeKeys.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    protected static boolean isBlank(List<String> mergeKeys) {
        return mergeKeys == null || mergeKeys.isEmpty()
            || mergeKeys.stream().allMatch(s -> s == null || s.trim().isEmpty());
    }

    protected static List<String> nonKeyColumns(List<String> columns, List<String> keys) {
        return columns.stream().filter(c -> !keys.contains(c)).collect(Collectors.toList());
    }
}
