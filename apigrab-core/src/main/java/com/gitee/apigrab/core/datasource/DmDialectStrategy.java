package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.TargetType;

/**
 * 达梦 DM：Oracle 兼容方言族（双引号、DUAL、INSERT ALL / MERGE、ROWNUM）。
 *
 * <p>与 dbswitch 的 DM 产品实现一致，复用 Oracle 方言，只覆盖达梦自身的数据字典
 * 与布尔类型差异。
 */
public class DmDialectStrategy extends OracleDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.DM;
    }

    @Override
    public String getSchemaListQuery() {
        // 达梦的 JDBC getSchemas 常返回空，参照 sqlrest 直接查 ALL_OBJECTS 中的 SCH 对象。
        return "SELECT DISTINCT object_name FROM ALL_OBJECTS WHERE OBJECT_TYPE = 'SCH'";
    }

    @Override
    public String getTableListQuery() {
        // 达梦提供 Oracle 兼容的 USER_TABLES / USER_VIEWS 数据字典视图。
        return "SELECT table_name FROM user_tables UNION SELECT view_name FROM user_views";
    }

    @Override
    protected String mapColumnType(FieldMappingEntity mapping) {
        // 达梦没有 TINYINT(1) 布尔别名，用 BIT 表示布尔。
        if (mapping.getTargetType() == TargetType.BOOLEAN) {
            return "BIT";
        }
        return super.mapColumnType(mapping);
    }
}
