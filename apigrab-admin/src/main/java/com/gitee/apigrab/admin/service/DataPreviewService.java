package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.core.datasource.DynamicDataSourceManager;
import com.gitee.apigrab.core.datasource.SqlDialect;
import com.gitee.apigrab.persistence.dao.CollectTaskDao;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.dao.ExecutionLogDao;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only preview of already-ingested data for an execution's target table.
 */
@Service
public class DataPreviewService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    @Resource
    private ExecutionLogDao executionLogDao;

    @Resource
    private CollectTaskDao collectTaskDao;

    @Resource
    private DatasourceConfigDao datasourceConfigDao;

    @Resource
    private DynamicDataSourceManager dataSourceManager;

    public List<Map<String, Object>> preview(Long executionId, Integer limit) {
        ExecutionLogEntity logEntity = executionLogDao.getById(executionId);
        if (logEntity == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "执行记录不存在");
        }
        CollectTaskEntity task = collectTaskDao.getById(logEntity.getTaskId());
        if (task == null || task.getTargetTable() == null || task.getTargetTable().isEmpty()) {
            return new ArrayList<>();
        }
        int effectiveLimit = Optional.ofNullable(limit).filter(l -> l > 0).map(l -> Math.min(l, MAX_LIMIT)).orElse(DEFAULT_LIMIT);
        DbType dbType = resolveDbType(task.getDatasourceId());
        // 与入库侧一致：先拼出全限定名（schema.table），再按目标库约定加包裹符，
        // 例如 PostgreSQL/Oracle 得到 "schema"."table"，MySQL 得到 `schema`.`table`。
        String table = SqlDialect.quote(qualifiedTableName(task), dbType);
        // 预览分页也走方言：Oracle / 达梦用 ROWNUM，SQL Server 用 TOP，其余用 LIMIT。
        String sql = SqlDialect.resolve(dbType).buildPreviewSql(table, effectiveLimit);

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSourceManager.getConnection(task.getDatasourceId());
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            throw new CommonException(ResponseErrorCode.INTERNAL_ERROR, "预览入库数据失败: " + e.getMessage());
        }
        return rows;
    }

    /** 目标表全限定名：有 schema 时拼为 {@code schema.table}，与入库侧 DataPersister 保持一致。 */
    private String qualifiedTableName(CollectTaskEntity task) {
        String table = sanitizeIdentifier(task.getTargetTable());
        String schema = task.getTargetSchema();
        if (schema != null && !schema.trim().isEmpty()) {
            return sanitizeIdentifier(schema.trim()) + "." + table;
        }
        return table;
    }

    private DbType resolveDbType(Long datasourceId) {
        if (datasourceId == null) {
            return null;
        }
        DatasourceConfigEntity config = datasourceConfigDao.getById(datasourceId);
        return config == null ? null : config.getDbType();
    }

    private String sanitizeIdentifier(String identifier) {
        if (identifier == null || !identifier.matches("[A-Za-z0-9_$.]+")) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "非法的目标表名: " + identifier);
        }
        return identifier;
    }
}
