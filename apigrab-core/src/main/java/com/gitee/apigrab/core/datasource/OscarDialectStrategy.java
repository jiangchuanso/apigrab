package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.TargetType;

/**
 * 神通 Oscar：按 sqlrest 的归类走 PostgreSQL 兼容方言族（双引号、
 * information_schema.schemata、LIMIT/OFFSET）。
 *
 * <p>布尔类型按 dbswitch 的 Oscar 字段定义使用 BIT；长文本沿用 TEXT。
 * 若目标库版本不支持 ON CONFLICT，MERGE 写入模式请改用 INSERT / TRUNCATE_INSERT。
 */
public class OscarDialectStrategy extends PostgreSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.OSCAR;
    }

    @Override
    protected String mapColumnType(FieldMappingEntity mapping) {
        if (mapping.getTargetType() == TargetType.BOOLEAN) {
            return "BIT";
        }
        return super.mapColumnType(mapping);
    }
}
