package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.util.PasswordUtils;
import com.gitee.apigrab.core.datasource.DynamicDataSourceManager;
import com.gitee.apigrab.core.datasource.SqlDialect;
import com.gitee.apigrab.core.datasource.SqlDialectStrategy;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class DatasourceService {

    /** 与列定义 `rc_datasource_config.name VARCHAR(100)` 对应（design C10）。就近定义：各表宽度可独立变化。 */
    private static final int NAME_MAX_LENGTH = 100;

    @Resource
    private DatasourceConfigDao datasourceConfigDao;

    @Resource
    private DynamicDataSourceManager dataSourceManager;

    public DatasourceConfigEntity getById(Long id) {
        DatasourceConfigEntity entity = datasourceConfigDao.getById(id);
        if (entity == null) {
            throw new CommonException(ResponseErrorCode.DATASOURCE_NOT_FOUND, "数据源不存在");
        }
        return entity;
    }

    public List<DatasourceConfigEntity> list(DatasourceConfigEntity param) {
        return datasourceConfigDao.list(param);
    }

    public void create(DatasourceConfigEntity entity) {
        entity.setName(requireUniqueName(entity.getName(), null));
        // AES 加密密码
        entity.setPassword(PasswordUtils.encrypt(entity.getPassword()));
        if (entity.getStatus() == null) {
            entity.setStatus(EnableStatus.ENABLED);
        }
        datasourceConfigDao.insert(entity);
        // 连接池按需构建：此处不预建，首次使用时再创建
    }

    public void update(DatasourceConfigEntity entity) {
        entity.setName(requireUniqueName(entity.getName(), entity.getId()));
        if (entity.getPassword() != null && !entity.getPassword().startsWith("$ENC$")) {
            // 编辑回显的是库中密文；未修改时保持原密文，避免二次加密
            DatasourceConfigEntity existing = datasourceConfigDao.getById(entity.getId());
            boolean unchangedCipher = existing != null && entity.getPassword().equals(existing.getPassword());
            if (!unchangedCipher) {
                entity.setPassword(PasswordUtils.encrypt(entity.getPassword()));
            }
        }
        datasourceConfigDao.updateById(entity);
        // 配置变更：失效缓存，下次使用按新配置重建
        dataSourceManager.destroyDataSource(entity.getId());
    }

    public void delete(Long id) {
        // 引用保护：被采集任务引用的数据源不得删除（design C4）——先校验，未通过则不销毁连接池、不删记录
        if (datasourceConfigDao.countReferences(id) > 0) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "该数据源已被任务引用，无法删除");
        }
        dataSourceManager.destroyDataSource(id);
        datasourceConfigDao.deleteById(id);
    }

    /**
     * 名称归一与唯一性校验，返回 trim 后的名称（design C3/C4/C5/C6）。
     *
     * <p>数据源早先已有 {@code uk_name} 唯一索引，但缺少服务层校验：重名会直接抛出数据库约束
     * 异常，经 {@code GlobalExceptionHandler} 落到 HTTP 500 与原始 SQL 报文。本方法把该情形
     * 转为可读的 400 提示。
     *
     * <p>判重依赖 {@code getByName} 的 SQL 等值查询，**不在 Java 层比较字符串**：表使用 MySQL
     * 默认（大小写不敏感）排序规则，SQL 等值天然忽略大小写，与唯一索引判定一致。
     *
     * @param selfId 更新时传入自身 id 以排除自己；新增时传 {@code null}
     */
    private String requireUniqueName(String rawName, Long selfId) {
        String name = rawName == null ? null : rawName.trim();
        if (name == null || name.isEmpty()) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "数据源名称不能为空");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                "数据源名称长度不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }
        DatasourceConfigEntity existing = datasourceConfigDao.getByName(name);
        if (existing != null && (selfId == null || !selfId.equals(existing.getId()))) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "数据源名称已存在");
        }
        return name;
    }

    public boolean testConnection(Long id) {
        DatasourceConfigEntity entity = getById(id);
        SqlDialectStrategy dialect = SqlDialect.resolve(entity.getDbType());
        try (Connection conn = dataSourceManager.getConnection(id);
             Statement stmt = conn.createStatement()) {
            stmt.execute(dialect.getTestQuery());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 以给定配置做一次性连接测试（用于新建/编辑态，未保存也不落库）。
     */
    public boolean testConnection(DatasourceConfigEntity entity) {
        return dataSourceManager.testConnection(entity);
    }

    public List<String> getSchemas(Long id) {
        DatasourceConfigEntity entity = getById(id);
        SqlDialectStrategy dialect = SqlDialect.resolve(entity.getDbType());
        List<String> schemas = new ArrayList<>();
        try (Connection conn = dataSourceManager.getConnection(id);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(dialect.getSchemaListQuery())) {
            while (rs.next()) {
                schemas.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new CommonException(ResponseErrorCode.DATASOURCE_CONNECTION_FAILED, "获取库列表失败: " + e.getMessage());
        }
        return schemas;
    }

    public List<String> getTables(Long id) {
        return getTables(id, null);
    }

    public List<String> getTables(Long id, String schema) {
        if (schema == null || schema.trim().isEmpty()) {
            return listTablesByDialectQuery(id);
        }
        return listTablesByMetadata(id, schema);
    }

    private List<String> listTablesByDialectQuery(Long id) {
        DatasourceConfigEntity entity = getById(id);
        SqlDialectStrategy dialect = SqlDialect.resolve(entity.getDbType());
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSourceManager.getConnection(id);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(dialect.getTableListQuery())) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new CommonException(ResponseErrorCode.DATASOURCE_CONNECTION_FAILED, "获取表列表失败: " + e.getMessage());
        }
        return tables;
    }

    private List<String> listTablesByMetadata(Long id, String schema) {
        DatasourceConfigEntity entity = getById(id);
        boolean catalogScope = SqlDialect.resolve(entity.getDbType()).isCatalogScope();
        String catalog = catalogScope ? schema : null;
        String schemaParam = catalogScope ? null : schema;
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSourceManager.getConnection(id)) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(catalog, schemaParam, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            throw new CommonException(ResponseErrorCode.DATASOURCE_CONNECTION_FAILED, "获取表列表失败: " + e.getMessage());
        }
        return tables;
    }

    public List<Map<String, String>> getColumns(Long id, String schema, String tableName) {
        DatasourceConfigEntity entity = getById(id);
        boolean catalogScope = SqlDialect.resolve(entity.getDbType()).isCatalogScope();
        boolean hasSchema = schema != null && !schema.trim().isEmpty();
        String catalog = (hasSchema && catalogScope) ? schema : null;
        String schemaParam = (hasSchema && !catalogScope) ? schema : null;
        List<Map<String, String>> columns = new ArrayList<>();
        try (Connection conn = dataSourceManager.getConnection(id)) {
            DatabaseMetaData meta = conn.getMetaData();
            Set<String> primaryKeys = readPrimaryKeyColumns(meta, catalog, schemaParam, tableName);
            try (ResultSet rs = meta.getColumns(catalog, schemaParam, tableName, "%")) {
                while (rs.next()) {
                    Map<String, String> col = new HashMap<>();
                    String name = rs.getString("COLUMN_NAME");
                    col.put("name", name);
                    col.put("type", rs.getString("TYPE_NAME"));
                    col.put("size", String.valueOf(rs.getInt("COLUMN_SIZE")));
                    col.put("nullable", String.valueOf(rs.getInt("NULLABLE")));
                    col.put("remarks", rs.getString("REMARKS"));
                    // 主键标记供「关闭自动建表 + 选择已有表」时推导合并主键（design D2）；
                    // 仅只读元数据，不影响写入/建表语义。
                    col.put("primaryKey", primaryKeys.contains(name == null ? null : name.trim()) ? "true" : "false");
                    columns.add(col);
                }
            }
        } catch (Exception e) {
            throw new CommonException(ResponseErrorCode.DATASOURCE_CONNECTION_FAILED, "获取字段列表失败: " + e.getMessage());
        }
        return columns;
    }

    /**
     * 读取目标表主键列名集合（design D2）。
     *
     * <p>读取失败时按「无主键」处理并告警：列清单本身是「关闭自动建表 + 选择已有表」场景的
     * 基础能力，不得因主键元数据不可用而整体失败（design R4）。
     */
    private Set<String> readPrimaryKeyColumns(DatabaseMetaData meta, String catalog, String schemaParam, String tableName) {
        Set<String> keys = new HashSet<>();
        try (ResultSet rs = meta.getPrimaryKeys(catalog, schemaParam, tableName)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (name != null && !name.trim().isEmpty()) {
                    keys.add(name.trim());
                }
            }
        } catch (Exception e) {
            log.warn("读取表主键失败，按无主键处理: {}", e.getMessage());
        }
        return keys;
    }
}
