package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.ExecutionLogMapper;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionLogDaoTest {

    private ExecutionLogDao dao;
    private ExecutionLogMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        dao = new ExecutionLogDao();
        mapper = Mockito.mock(ExecutionLogMapper.class);
        java.lang.reflect.Field field = ExecutionLogDao.class.getDeclaredField("executionLogMapper");
        field.setAccessible(true);
        field.set(dao, mapper);
    }

    @Test
    void testGetById() {
        ExecutionLogEntity entity = new ExecutionLogEntity();
        entity.setId(1L);
        entity.setStatus(ExecutionStatus.SUCCESS);
        when(mapper.selectById(1L)).thenReturn(entity);

        ExecutionLogEntity result = dao.getById(1L);
        assertNotNull(result);
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    void testList() {
        ExecutionLogEntity param = new ExecutionLogEntity();
        when(mapper.selectList(param)).thenReturn(Arrays.asList(new ExecutionLogEntity()));
        assertEquals(1, dao.list(param).size());
    }

    @Test
    void testInsert() {
        ExecutionLogEntity entity = new ExecutionLogEntity();
        when(mapper.insert(entity)).thenReturn(1);
        assertEquals(1, dao.insert(entity));
    }

    @Test
    void testUpdateById() {
        ExecutionLogEntity entity = new ExecutionLogEntity();
        when(mapper.updateById(entity)).thenReturn(1);
        assertEquals(1, dao.updateById(entity));
    }

    @Test
    void testDeleteByTaskId() {
        when(mapper.deleteByTaskId(10L)).thenReturn(3);
        assertEquals(3, dao.deleteByTaskId(10L));
    }

    @Test
    void testCountRunningByTaskId() {
        when(mapper.countRunningByTaskId(10L)).thenReturn(2L);
        assertEquals(2L, dao.countRunningByTaskId(10L));
    }
}
