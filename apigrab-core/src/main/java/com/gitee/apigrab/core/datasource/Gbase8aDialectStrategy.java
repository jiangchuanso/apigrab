package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;

/**
 * 南大通用 GBase8A：MySQL 协议兼容方言族（反引号、多 VALUES、ON DUPLICATE KEY）。
 *
 * <p>数据库对应 JDBC catalog（isCatalogScope=true 继承自 MySQL 方言）；
 * 表清单改用 information_schema，避免部分版本不支持 SHOW FULL TABLES。
 */
public class Gbase8aDialectStrategy extends MySqlDialectStrategy {

    @Override
    public boolean supports(DbType dbType) {
        return dbType == DbType.GBASE8A;
    }

    @Override
    public String getTableListQuery() {
        return "SELECT table_name FROM information_schema.tables WHERE table_schema = database()";
    }
}
