package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.ExecutionStageLogMapper;
import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionStageLogDaoTest {

    private ExecutionStageLogDao dao;
    private ExecutionStageLogMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        dao = new ExecutionStageLogDao();
        mapper = Mockito.mock(ExecutionStageLogMapper.class);
        java.lang.reflect.Field field = ExecutionStageLogDao.class.getDeclaredField("executionStageLogMapper");
        field.setAccessible(true);
        field.set(dao, mapper);
    }

    @Test
    void testInsert() {
        ExecutionStageLogEntity entity = new ExecutionStageLogEntity();
        when(mapper.insert(entity)).thenReturn(1);
        assertEquals(1, dao.insert(entity));
    }

    @Test
    void testGetByExecutionId() {
        when(mapper.selectByExecutionId(7L)).thenReturn(Arrays.asList(new ExecutionStageLogEntity()));
        assertEquals(1, dao.getByExecutionId(7L).size());
    }

    @Test
    void testDeleteByTaskId() {
        when(mapper.deleteByTaskId(10L)).thenReturn(2);
        assertEquals(2, dao.deleteByTaskId(10L));
    }
}
