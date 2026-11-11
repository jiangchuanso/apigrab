package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;

/**
 * 人大金仓 KingbaseES：PostgreSQL 兼容方言族。
 *
 * <p>参照 sqlrest / dbswitch 的归类：双引号、information_schema、LIMIT、
 * TEXT/BOOLEAN、ON CONFLICT 均可复用 PostgreSQL 实现。
 */
public class KingbaseDialectStrategy extends PostgreSqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.KINGBASE;
    }
}
