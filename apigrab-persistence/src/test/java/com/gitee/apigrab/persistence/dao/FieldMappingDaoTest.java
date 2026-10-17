package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.FieldMappingMapper;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FieldMappingDaoTest {

    private FieldMappingDao dao;
    private FieldMappingMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        dao = new FieldMappingDao();
        mapper = Mockito.mock(FieldMappingMapper.class);
        java.lang.reflect.Field field = FieldMappingDao.class.getDeclaredField("fieldMappingMapper");
        field.setAccessible(true);
        field.set(dao, mapper);
    }

    @Test
    void testGetByTaskId() {
        FieldMappingEntity e1 = new FieldMappingEntity();
        e1.setId(1L);
        e1.setSourceField("$.name");
        e1.setTargetField("user_name");
        when(mapper.selectByTaskId(10L)).thenReturn(Arrays.asList(e1));

        List<FieldMappingEntity> result = dao.getByTaskId(10L);
        assertEquals(1, result.size());
        assertEquals("$.name", result.get(0).getSourceField());
    }

    @Test
    void testBatchInsert() {
        List<FieldMappingEntity> list = Arrays.asList(new FieldMappingEntity(), new FieldMappingEntity());
        when(mapper.batchInsert(list)).thenReturn(2);
        assertEquals(2, dao.batchInsert(list));
    }

    @Test
    void testDeleteByTaskId() {
        when(mapper.deleteByTaskId(10L)).thenReturn(3);
        assertEquals(3, dao.deleteByTaskId(10L));
    }
}
