package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AlarmConfigEntityTest {

    @Test
    void testGettersAndSetters() {
        AlarmConfigEntity entity = new AlarmConfigEntity();
        Date now = new Date();

        entity.setId(1L);
        entity.setStatus(EnableStatus.ENABLED);
        entity.setEndpoint("https://webhook.example.com/alert");
        entity.setContentType("application/json");
        entity.setInputTemplate("{\"text\":\"任务 ${taskName} 执行${status}，耗时 ${costMs}ms\"}");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(1L, entity.getId());
        assertEquals(EnableStatus.ENABLED, entity.getStatus());
        assertEquals("https://webhook.example.com/alert", entity.getEndpoint());
        assertEquals("application/json", entity.getContentType());
        assertTrue(entity.getInputTemplate().contains("${taskName}"));
    }

    @Test
    void testDefaultValues() {
        AlarmConfigEntity entity = new AlarmConfigEntity();
        assertNull(entity.getId());
        assertNull(entity.getStatus());
        assertNull(entity.getEndpoint());
    }
}
