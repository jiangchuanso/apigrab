package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionLogEntityTest {

    @Test
    void testGettersAndSetters() {
        ExecutionLogEntity entity = new ExecutionLogEntity();
        Date now = new Date();

        entity.setId(1L);
        entity.setTaskId(100L);
        entity.setStatus(ExecutionStatus.SUCCESS);
        entity.setStartTime(now);
        entity.setEndTime(now);
        entity.setTotalRecords(500);
        entity.setSuccessCount(498);
        entity.setFailCount(2);
        entity.setCostMs(3500L);
        entity.setPageCount(5);
        entity.setCreateTime(now);

        assertEquals(1L, entity.getId());
        assertEquals(100L, entity.getTaskId());
        assertEquals(ExecutionStatus.SUCCESS, entity.getStatus());
        assertEquals(500, entity.getTotalRecords());
        assertEquals(5, entity.getPageCount());
    }

    @Test
    void testFailedExecution() {
        ExecutionLogEntity entity = new ExecutionLogEntity();
        entity.setStatus(ExecutionStatus.FAILED);
        entity.setErrorMsg("Connection timeout");

        assertEquals(ExecutionStatus.FAILED, entity.getStatus());
        assertEquals("Connection timeout", entity.getErrorMsg());
    }
}
