package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatasourceConfigEntityTest {

    @Test
    void testGettersAndSetters() {
        DatasourceConfigEntity entity = new DatasourceConfigEntity();
        Date now = new Date();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("useSSL", false);

        entity.setId(1L);
        entity.setName("prod mysql");
        entity.setDbType(DbType.MYSQL);
        entity.setDbVersion("8.0");
        entity.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        entity.setDriverClass("com.mysql.cj.jdbc.Driver");
        entity.setDriverPath("/opt/jdbc/mysql.jar");
        entity.setUsername("root");
        entity.setPassword("enc");
        entity.setPoolMaxSize(20);
        entity.setPoolMinIdle(5);
        entity.setConnProps(props);
        entity.setStatus(EnableStatus.ENABLED);
        entity.setRemark("prod");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(1L, entity.getId());
        assertEquals(DbType.MYSQL, entity.getDbType());
        assertEquals("8.0", entity.getDbVersion());
        assertEquals(20, entity.getPoolMaxSize());
        assertEquals(Boolean.FALSE, entity.getConnProps().get("useSSL"));
        assertEquals(EnableStatus.ENABLED, entity.getStatus());
    }

    @Test
    void testDefaultValues() {
        DatasourceConfigEntity entity = new DatasourceConfigEntity();
        assertNull(entity.getId());
        assertNull(entity.getDbType());
        assertNull(entity.getPoolMaxSize());
    }
}
