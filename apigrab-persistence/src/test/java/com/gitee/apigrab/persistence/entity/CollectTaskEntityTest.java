package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import com.gitee.apigrab.persistence.entity.enums.ScheduleType;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CollectTaskEntityTest {

    @Test
    void testGettersAndSetters() {
        CollectTaskEntity entity = new CollectTaskEntity();
        Date now = new Date();

        entity.setId(1L);
        entity.setName("daily sync");
        entity.setApiConfigId(10L);
        entity.setDatasourceId(20L);
        entity.setTargetSchema("public");
        entity.setTargetTable("t_data");
        entity.setWriteMode(WriteMode.MERGE);
        entity.setMergeKeys("id,name");
        entity.setAutoCreateTable(1);
        entity.setBatchSize(500);
        entity.setRetryTimes(2);
        entity.setTimeoutMs(60000L);
        entity.setAlarmEnabled(1);
        entity.setScheduleType(ScheduleType.CRON);
        entity.setCronExpression("0 0 2 * * ?");
        entity.setStatus(TaskStatus.RUNNING);
        entity.setRemark("remark");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getApiConfigId());
        assertEquals("public", entity.getTargetSchema());
        assertEquals(WriteMode.MERGE, entity.getWriteMode());
        assertEquals("id,name", entity.getMergeKeys());
        assertEquals(500, entity.getBatchSize());
        assertEquals(60000L, entity.getTimeoutMs());
        assertEquals(ScheduleType.CRON, entity.getScheduleType());
        assertEquals(TaskStatus.RUNNING, entity.getStatus());
    }

    @Test
    void testDefaultValues() {
        CollectTaskEntity entity = new CollectTaskEntity();
        assertNull(entity.getId());
        assertNull(entity.getApiConfigId());
        assertNull(entity.getBatchSize());
        assertNull(entity.getCronExpression());
    }
}
