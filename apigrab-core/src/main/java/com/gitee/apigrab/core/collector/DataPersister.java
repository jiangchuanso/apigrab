package com.gitee.apigrab.core.collector;

import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.core.datasource.DynamicDataSourceManager;
import com.gitee.apigrab.core.datasource.SqlDialect;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DataPersister {

    @Resource
    private DynamicDataSourceManager dataSourceManager;

    public DataWriteResult persist(List<Map<String, Object>> records, CollectTaskEntity task,
                                      DatasourceConfigEntity dsConfig, List<FieldMappingEntity> mappings,
                                      FailureSampleCollector failureCollector) {
        if (records == null || records.isEmpty()) {
            return new DataWriteResult();
        }
        try (WriteSession session = openSession(task, dsConfig, mappings)) {
            session.prepare();
            return session.write(records, failureCollector);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Data persist failed", e);
            throw new RuntimeException("Data persist failed: " + e.getMessage(), e);
        }
    }

    /**
     * 打开一次执行级的写库会话：持有目标库连接，建表/清表在整个执行内仅准备一次，
     * 之后可逐页反复写入。调用方须在 {@code finally} 中关闭（实现 {@link AutoCloseable}）。
     */
    public WriteSession openSession(CollectTaskEntity task, DatasourceConfigEntity dsConfig,
                                    List<FieldMappingEntity> mappings) {
        Connection conn;
        try {
            conn = dataSourceManager.getConnection(task.getDatasourceId());
        } catch (SQLException e) {
            log.error("Data persist failed", e);
            throw new RuntimeException("Data persist failed: " + e.getMessage(), e);
        }
        return new WriteSession(conn, task, dsConfig, mappings);
    }

    /**
     * 执行级写库会话。
     *
     * <p>{@link #prepare()} 负责一次性完成「建表（autoCreateTable）」与「清表（TRUNCATE_INSERT）」，
     * 因此逐页调用 {@link #write} 不会反复清空目标表。{@link #write} 内部仍按 {@code batchSize}
     * 分批写入，批失败计入失败并继续（与既有整批写库语义一致）。
     */
    public class WriteSession implements AutoCloseable {

        private final Connection conn;
        private final CollectTaskEntity task;
        private final DatasourceConfigEntity dsConfig;
        private final List<FieldMappingEntity> mappings;
        private final DbType dbType;
        private final WriteMode writeMode;
        private final String targetTable;
        private final List<String> columns;
        private final List<String> mergeKeys;
        private final int batchSize;
        private boolean prepared;

        WriteSession(Connection conn, CollectTaskEntity task, DatasourceConfigEntity dsConfig,
                     List<FieldMappingEntity> mappings) {
            this.conn = conn;
            this.task = task;
            this.dsConfig = dsConfig;
            this.mappings = mappings;
            this.dbType = dsConfig.getDbType();
            this.writeMode = Optional.ofNullable(task.getWriteMode()).orElse(WriteMode.INSERT);
            this.targetTable = effectiveTable(task);
            this.columns = mappings.stream()
                .map(FieldMappingEntity::getTargetField)
                .collect(Collectors.toList());
            this.mergeKeys = parseMergeKeys(task.getMergeKeys());
            this.batchSize = Optional.ofNullable(task.getBatchSize()).orElse(500);
        }

        /** 一次性准备：建表（如需）+ 清表（TRUNCATE_INSERT）。幂等，可安全重复调用。 */
        public void prepare() {
            if (prepared) {
                return;
            }
            try {
                if (task.getAutoCreateTable() != null && task.getAutoCreateTable() == 1) {
                    ensureTableExists(conn, task, dsConfig, mappings);
                }
                if (writeMode == WriteMode.TRUNCATE_INSERT) {
                    truncateTable(conn, targetTable, dbType);
                }
                prepared = true;
            } catch (Exception e) {
                log.error("Data persist prepare failed", e);
                throw new RuntimeException("Data persist failed: " + e.getMessage(), e);
            }
        }

        /** 写入一批记录（内部按 batchSize 再分批）。批失败不计入异常，计入失败条数并继续。 */
        public DataWriteResult write(List<Map<String, Object>> records, FailureSampleCollector failureCollector) {
            DataWriteResult result = new DataWriteResult();
            if (records == null || records.isEmpty()) {
                return result;
            }
            prepare();
            for (int i = 0; i < records.size(); i += batchSize) {
                int end = Math.min(i + batchSize, records.size());
                List<Map<String, Object>> batch = records.subList(i, end);
                try {
                    int count = batchInsert(conn, dbType, targetTable, columns, mergeKeys, batch, writeMode);
                    result.setSuccessCount(result.getSuccessCount() + count);
                } catch (Exception e) {
                    log.error("Batch insert failed for records {}-{}", i, end, e);
                    result.setFailCount(result.getFailCount() + batch.size());
                    if (failureCollector != null && !batch.isEmpty()) {
                        failureCollector.add(e.getMessage(), batch.get(0));
                    }
                }
            }
            return result;
        }

        @Override
        public void close() {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Failed to close write session connection: {}", e.getMessage());
                }
            }
        }
    }

    private void ensureTableExists(Connection conn, CollectTaskEntity task,
                                    DatasourceConfigEntity dsConfig, List<FieldMappingEntity> mappings) throws SQLException {
        String tableName = task.getTargetTable();
        String schema = task.getTargetSchema();
        DbType dbType = dsConfig.getDbType();
        boolean hasSchema = schema != null && !schema.trim().isEmpty();
        boolean catalogScope = SqlDialect.resolve(dbType).isCatalogScope();
        String catalog = (hasSchema && catalogScope) ? schema : null;
        String schemaParam = (hasSchema && !catalogScope) ? schema : null;

        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(catalog, schemaParam, tableName, null)) {
            if (!rs.next()) {
                // 合并主键在本系统中即表主键：同一份配置既驱动 Upsert 冲突键，也驱动建表主键
                List<String> primaryKeys = parseMergeKeys(task.getMergeKeys());
                String ddl = SqlDialect.generateCreateTableSql(effectiveTable(task), mappings, primaryKeys, dbType);
                log.info("Creating table: {}", ddl);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(ddl);
                }
            }
        }
    }

    private String effectiveTable(CollectTaskEntity task) {
        String schema = task.getTargetSchema();
        if (schema != null && !schema.trim().isEmpty()) {
            return schema.trim() + "." + task.getTargetTable();
        }
        return task.getTargetTable();
    }

    private void truncateTable(Connection conn, String tableName, DbType dbType) throws SQLException {
        String sql = SqlDialect.truncateSql(tableName, dbType);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private List<String> parseMergeKeys(String mergeKeys) {
        if (mergeKeys == null || mergeKeys.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> keys = new ArrayList<>();
        for (String key : mergeKeys.split(",")) {
            if (key.trim().length() > 0) {
                keys.add(key.trim());
            }
        }
        return keys;
    }

    private int batchInsert(Connection conn, DbType dbType, String tableName, List<String> columns,
                            List<String> mergeKeys, List<Map<String, Object>> batch, WriteMode writeMode) throws SQLException {
        if (batch.isEmpty()) return 0;

        String sql = SqlDialect.buildWriteSql(dbType, writeMode, tableName, columns, mergeKeys, batch.size());

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            for (Map<String, Object> record : batch) {
                for (String column : columns) {
                    Object value = record.get(column);
                    if (value != null) {
                        if (value instanceof Number) {
                            if (value instanceof Integer) ps.setInt(paramIndex, (Integer) value);
                            else if (value instanceof Long) ps.setLong(paramIndex, (Long) value);
                            else if (value instanceof Double) ps.setDouble(paramIndex, (Double) value);
                            else ps.setString(paramIndex, value.toString());
                        } else if (value instanceof java.util.Date) {
                            ps.setTimestamp(paramIndex, new Timestamp(((java.util.Date) value).getTime()));
                        } else {
                            ps.setString(paramIndex, value.toString());
                        }
                    } else {
                        ps.setNull(paramIndex, Types.VARCHAR);
                    }
                    paramIndex++;
                }
            }
            return ps.executeUpdate();
        }
    }
}
