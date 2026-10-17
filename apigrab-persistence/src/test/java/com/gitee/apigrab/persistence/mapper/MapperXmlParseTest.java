package com.gitee.apigrab.persistence.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Parses every Mapper XML offline (no DB) to catch MyBatis builder errors such as
 * type-handler instantiation failures (which occur at SqlSessionFactory build time).
 */
class MapperXmlParseTest {

    @Test
    void allMapperXmlParsable() throws Exception {
        Configuration cfg = new Configuration();
        String[] resources = {
            "mapper/ApiConfigMapper.xml",
            "mapper/AuthConfigMapper.xml",
            "mapper/DatasourceConfigMapper.xml",
            "mapper/FieldMappingMapper.xml",
            "mapper/CollectTaskMapper.xml",
            "mapper/ExecutionLogMapper.xml",
            "mapper/ExecutionStageLogMapper.xml",
            "mapper/AlarmConfigMapper.xml"
        };
        for (String resource : resources) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(is, "missing resource: " + resource);
                new XMLMapperBuilder(is, cfg, resource, cfg.getSqlFragments()).parse();
            }
        }
    }
}
