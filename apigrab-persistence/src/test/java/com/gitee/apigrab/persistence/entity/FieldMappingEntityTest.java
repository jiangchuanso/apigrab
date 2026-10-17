package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.TargetType;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class FieldMappingEntityTest {

    @Test
    void testGettersAndSetters() {
        FieldMappingEntity entity = new FieldMappingEntity();
        Date now = new Date();

        entity.setId(1L);
        entity.setTaskId(10L);
        entity.setSourceField("$.name");
        entity.setTargetField("user_name");
        entity.setTargetType(TargetType.VARCHAR);
        entity.setTargetLength(255);
        entity.setNullable(0);
        entity.setDefaultValue("N/A");
        entity.setConvertExpr("trim");
        entity.setSortOrder(1);
        entity.setCreateTime(now);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getTaskId());
        assertEquals("$.name", entity.getSourceField());
        assertEquals("user_name", entity.getTargetField());
        assertEquals(TargetType.VARCHAR, entity.getTargetType());
        assertEquals(255, entity.getTargetLength());
        assertEquals(0, entity.getNullable());
        assertEquals("N/A", entity.getDefaultValue());
        assertEquals("trim", entity.getConvertExpr());
        assertEquals(1, entity.getSortOrder());
    }

    @Test
    void testDefaultValues() {
        FieldMappingEntity entity = new FieldMappingEntity();
        assertNull(entity.getId());
        assertNull(entity.getSourceField());
        assertNull(entity.getTargetType());
        assertNull(entity.getConvertExpr());
    }
}
